package com.viaibot.ai.service

import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.document.Document
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

@Service
class AiEmbeddingService(private val vectorStore: VectorStore) {
    fun embed(documentResource: Resource) {
        val randomAccess = RandomAccessReadBuffer(documentResource.inputStream)

        val splitter = TokenTextSplitter(
            800,
            100,
            100,
            1000,
            true
        )

        Loader.loadPDF(randomAccess).use { document ->
            val stripper = PDFTextStripper()
            val totalPages = document.numberOfPages

            for (page in 1..totalPages) {
                stripper.startPage = page
                stripper.endPage = page
                val text = stripper.getText(document)

                if (text.isNotBlank()) {
                    val doc = Document(text)
                    val chunks = splitter.apply(listOf(doc))
                    vectorStore.accept(chunks)
                }
            }
        }
    }
}