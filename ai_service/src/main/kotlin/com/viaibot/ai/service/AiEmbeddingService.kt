package com.viaibot.ai.service

import org.springframework.ai.reader.tika.TikaDocumentReader
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

@Service
class AiEmbeddingService(private val vectorStore: VectorStore) {
    fun embed(documentResource: Resource) {
        val reader = TikaDocumentReader(documentResource)
        val splitter = TokenTextSplitter(200, 50, 5, 1000, true)
        vectorStore.accept(splitter.apply(reader.get()))
    }
}