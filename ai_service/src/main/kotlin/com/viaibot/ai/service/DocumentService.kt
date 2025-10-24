package com.viaibot.ai.service

import com.viaibot.ai.entity.ProcessingStatus
import org.apache.pdfbox.Loader
import com.viaibot.ai.repository.VectorStoreRepository
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class DocumentService(
    private val vectorStoreRepository: VectorStoreRepository,
    private val aiEmbeddingService: AiEmbeddingService
) {

    fun getDocumentList(query: String?): List<Map<String, Any>> {
        return vectorStoreRepository.countGroupedByFilenameFiltered(query)
    }

    fun saveDocument(file: MultipartFile): Map<String, Any> {
        val randomAccess = RandomAccessReadBuffer(file.inputStream)
        val totalPages = Loader.loadPDF(randomAccess).pages.count
        val taskId = aiEmbeddingService.startTracking(totalPages)
        aiEmbeddingService.embed(file.resource, taskId)
        return mapOf("taskId" to taskId, "totalPages" to totalPages)
    }

    fun deleteDocument(filename: String) {
        vectorStoreRepository.deleteByFilename(filename)
    }

    fun getStatusDocumentEmbedding(taskId: UUID): ProcessingStatus? = aiEmbeddingService.getStatus(taskId)

    fun cancelEmbedding(taskId: UUID) {
        aiEmbeddingService.cancel(taskId)
    }
}