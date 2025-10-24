package com.viaibot.ai.service

import com.viaibot.ai.entity.ProcessingStatus
import com.viaibot.ai.repository.VectorStoreRepository
import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.document.Document
import org.springframework.core.io.Resource
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class AiEmbeddingService(
    private val vectorStore: VectorStore,
    private val vectorStoreRepository: VectorStoreRepository
    ) {

    private val progressMap = ConcurrentHashMap<UUID, ProcessingStatus>()

    fun startTracking(totalPages: Int): UUID {
        val id = UUID.randomUUID()
        progressMap[id] = ProcessingStatus(totalPages)
        return id
    }

    fun getStatus(taskId: UUID): ProcessingStatus? = progressMap[taskId]

    fun cancel(taskId: UUID) {
        progressMap[taskId]?.apply {
            cancelled = true
        }
    }

    @Async
    fun embed(documentResource: Resource, taskId: UUID) {
        val randomAccess = RandomAccessReadBuffer(documentResource.inputStream)

        val splitter = TokenTextSplitter(
            500,
            100,
            50,
            1000,
            true
        )

        Loader.loadPDF(randomAccess).use { document ->
            val stripper = PDFTextStripper()
            val totalPages = document.numberOfPages

            val status = progressMap[taskId] ?: return

            for (page in 1..totalPages) {
                if (status.cancelled) {
                    progressMap[taskId]?.finished = true
                    vectorStoreRepository.deleteByFilename(documentResource.filename.toString())
                    return
                }
                
                stripper.startPage = page
                stripper.endPage = page
                val text = stripper.getText(document)

                if (text.isNotBlank()) {
                    val doc = Document.builder()
                        .text(text)
                        .metadata("filename", documentResource.filename.toString())
                        .build()
                    val chunks = splitter.apply(listOf(doc))
                    vectorStore.accept(chunks)
                }

                progressMap[taskId]?.apply { processedPages++ }
            }
        }
        progressMap[taskId]?.apply { finished = true }
    }
}