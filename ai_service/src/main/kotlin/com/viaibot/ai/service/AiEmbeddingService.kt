package com.viaibot.ai.service

import com.viaibot.ai.entity.ProcessingStatus
import com.viaibot.ai.repository.VectorStoreRepository
import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class AiEmbeddingService(
    private val vectorStore: VectorStore,
    private val vectorStoreRepository: VectorStoreRepository,
    @Value("\${embedding.chunk-size:500}") private val chunkSize: Int,
    @Value("\${embedding.min-chunk-size-chars:100}") private val minChunkSizeChars: Int,
    @Value("\${embedding.min-chunk-length-to-embed:50}") private val minChunkLengthToEmbed: Int,
    @Value("\${embedding.max-chunks:1000}") private val maxNumChunks: Int,
    @Value("\${embedding.keep-separator:true}") private val keepSeparator: Boolean,
    @Value("\${embedding.batch-size:5}") private val batchSize: Int
) {
    private val log = LoggerFactory.getLogger(AiEmbeddingService::class.java)

    private val progressMap = ConcurrentHashMap<UUID, ProcessingStatus>()

    companion object {
        private const val CLEANUP_INTERVAL_MS = 3_600_000L // 1 час
        private var lastCleanupTime = System.currentTimeMillis()
    }

    fun startTracking(totalPages: Int): UUID {
        val id = UUID.randomUUID()
        progressMap[id] = ProcessingStatus(totalPages)
        cleanupOldEntries()
        return id
    }

    fun getStatus(taskId: UUID): ProcessingStatus? = progressMap[taskId]

    fun cancel(taskId: UUID) {
        progressMap[taskId]?.apply {
            cancelled = true
        }
        log.info("Processing cancelled for taskId: $taskId")
    }

    @Async
    fun embed(fileBytes: ByteArray, filename: String, taskId: UUID) {
        log.info("Starting document processing: $filename, taskId: $taskId, size: ${fileBytes.size} bytes")

        val splitter = TokenTextSplitter(
            chunkSize,
            minChunkSizeChars,
            minChunkLengthToEmbed,
            maxNumChunks,
            keepSeparator
        )

        val randomAccess = RandomAccessReadBuffer(ByteArrayInputStream(fileBytes))
        try {
            randomAccess.use {
                Loader.loadPDF(it).use { document ->
                    processDocument(document, taskId, splitter, filename)
                }
            }
        } catch (e: Exception) {
            log.error("Error processing document: $filename", e)
            progressMap[taskId]?.apply {
                finished = true
                failed = true
                errorMessage = e.message ?: "Unknown error"
            }
            vectorStoreRepository.deleteByFilename(filename)
            throw e
        } finally {
            cleanupOldEntries()
        }
    }

    private fun processDocument(
        document: org.apache.pdfbox.pdmodel.PDDocument,
        taskId: UUID,
        splitter: TokenTextSplitter,
        filename: String
    ) {
        val totalPages = document.numberOfPages
        val status = progressMap[taskId] ?: run {
            log.warn("Status not found for taskId: $taskId")
            return
        }

        val allDocuments = mutableListOf<Document>()
        val stripper = PDFTextStripper()

        // Этап 1: Извлечение текста из страниц
        log.info("Extracting text from $totalPages pages...")
        for (page in 1..totalPages) {
            if (status.cancelled) {
                log.info("Processing cancelled for taskId: $taskId, filename: $filename")
                handleCancellation(taskId, filename)
                return
            }

            stripper.startPage = page
            stripper.endPage = page
            val text = stripper.getText(document)

            if (text.isNotBlank()) {
                val doc = Document.builder()
                    .text(text)
                    .metadata("filename", filename)
                    .metadata("page", page.toString())
                    .build()
                allDocuments.add(doc)
            }

            status.processedPages++
        }

        log.info("Text extraction completed. Processing ${allDocuments.size} pages with content.")

        // Этап 2: Разбиение на чанки
        val allChunks = allDocuments.flatMap { splitter.apply(listOf(it)) }
        status.totalChunks = allChunks.size
        log.info("Total chunks created for $filename: ${allChunks.size}")

        // Этап 3: Отправка батчами в векторное хранилище
        val batches = allChunks.chunked(batchSize)
        log.info("Uploading ${batches.size} batches to vector store...")

        batches.forEachIndexed { batchIndex, batch ->
            if (status.cancelled) {
                log.info("Processing cancelled at batch $batchIndex for taskId: $taskId")
                handleCancellation(taskId, filename)
                return
            }

            try {
                vectorStore.accept(batch)
                status.embeddedChunks += batch.size
                log.debug("Batch ${batchIndex + 1}/${batches.size} uploaded (${status.embeddedChunks}/${status.totalChunks} chunks)")
            } catch (e: Exception) {
                log.error("Error uploading batch ${batchIndex + 1} for $filename", e)
                status.failed = true
                status.errorMessage = e.message ?: "Failed to upload batch"
                vectorStoreRepository.deleteByFilename(filename)
                throw e
            }
        }

        status.finished = true
        log.info("Document processing completed: $filename, total chunks: ${allChunks.size}")
    }

    private fun handleCancellation(taskId: UUID, filename: String) {
        progressMap[taskId]?.apply {
            finished = true
        }
        vectorStoreRepository.deleteByFilename(filename)
        log.info("Cancelled processing cleaned up for: $filename")
    }

    /**
     * Очистка старых записей из progressMap для предотвращения утечки памяти
     */
    private fun cleanupOldEntries() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            val completedTasks = progressMap.entries.filter { it.value.finished }.map { it.key }
            completedTasks.forEach { progressMap.remove(it) }
            lastCleanupTime = currentTime
            log.debug("Cleaned up ${completedTasks.size} completed tasks from progressMap")
        }
    }
}