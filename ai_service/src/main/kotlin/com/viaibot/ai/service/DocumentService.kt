package com.viaibot.ai.service

import com.viaibot.ai.entity.ProcessingStatus
import org.apache.pdfbox.Loader
import com.viaibot.ai.repository.VectorStoreRepository
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.util.UUID

@Service
class DocumentService(
    private val vectorStoreRepository: VectorStoreRepository,
    private val aiEmbeddingService: AiEmbeddingService
) {
    private val log = LoggerFactory.getLogger(DocumentService::class.java)

    fun getDocumentList(query: String?): List<Map<String, Any>> {
        return vectorStoreRepository.countGroupedByFilenameFiltered(query)
    }

    fun saveDocument(file: MultipartFile): Map<String, Any> {
        // Копируем содержимое файла в память, чтобы избежать проблем с временными файлами
        val fileBytes = file.bytes
        val randomAccess = RandomAccessReadBuffer(ByteArrayInputStream(fileBytes))
        val totalPages = randomAccess.use { ra ->
            Loader.loadPDF(ra).pages.count
        }
        val taskId = aiEmbeddingService.startTracking(totalPages)
        aiEmbeddingService.embed(fileBytes, file.originalFilename ?: "unknown", taskId)
        return mapOf("taskId" to taskId, "totalPages" to totalPages)
    }

    fun deleteDocument(filename: String) {
        log.info("Deleting document: $filename")
        vectorStoreRepository.deleteByFilename(filename)
        log.info("Document deleted: $filename")
    }

    fun getStatusDocumentEmbedding(taskId: UUID): ProcessingStatus? = aiEmbeddingService.getStatus(taskId)

    fun cancelEmbedding(taskId: UUID) {
        aiEmbeddingService.cancel(taskId)
    }
}