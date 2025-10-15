package com.viaibot.ai.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

@Service
class DocumentService(
    private val aiEmbeddingService: AiEmbeddingService
) : CommandLineRunner {
    @Value("classpath:/files/rag-test.pdf")
    private lateinit var resource: Resource

    override fun run(vararg args: String?) {
//        aiEmbeddingService.embed(resource)
    }
}