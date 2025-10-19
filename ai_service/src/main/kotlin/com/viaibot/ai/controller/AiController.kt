package com.viaibot.ai.controller

import com.viaibot.ai.entity.ProcessingStatus
import com.viaibot.ai.entity.dto.UserInputDto
import com.viaibot.common.kafka.dto.UserInputMessageDto
import com.viaibot.ai.service.AiChatService
import com.viaibot.ai.service.AiEmbeddingService
import com.viaibot.ai.service.DocumentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Controller
class AiController(
    private val documentService: DocumentService
) {

    @GetMapping("/documents")
    fun getDocuments(@RequestParam(required = false) query: String?, model: Model): String {
        model.addAttribute("documents", documentService.getDocumentList(query))
        model.addAttribute("query", query ?: "")
        return "DocumentsPage"
    }

    @PostMapping("/documents/upload")
    @ResponseBody
    fun uploadDocument(@RequestParam("file") file: MultipartFile): Map<String, Any> {
        return documentService.saveDocument(file)
    }

    @GetMapping("/documents/progress/{id}")
    @ResponseBody
    fun getProgress(@PathVariable id: UUID): ProcessingStatus? {
        return documentService.getStatusDocumentEmbedding(id)
    }

    @DeleteMapping("/documents/delete/{filename}")
    fun deleteDocument(@PathVariable() filename: String): ResponseEntity<Unit> {
        documentService.deleteDocument(filename)
        return ResponseEntity.noContent().build()
    }
}