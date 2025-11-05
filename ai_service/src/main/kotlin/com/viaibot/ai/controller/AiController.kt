package com.viaibot.ai.controller

import com.viaibot.ai.config.AiChatOptionsConfig
import com.viaibot.ai.entity.ProcessingStatus
import com.viaibot.ai.entity.dto.AiConfigDto
import com.viaibot.ai.service.DocumentService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Controller
class AiController(
    private val documentService: DocumentService,
    private val aiConfig: AiChatOptionsConfig
) {

    @GetMapping("/config")
    fun getConfig(): AiConfigDto = aiConfig.get()

    @PostMapping("/config")
    @ResponseBody
    fun updateConfig(@RequestBody newConfig: AiConfigDto): ResponseEntity<AiConfigDto> {
        require(newConfig.temperature in 0.0..1.0) { "Temperature must be between 0.0 and 1.0" }

        aiConfig.update(newConfig)
        return ResponseEntity.ok(aiConfig.get())
    }

    @GetMapping("/")
    fun index(): String {
        return "redirect:/documents"
    }

    @GetMapping("/documents")
    fun getDocuments(
        @RequestParam(required = false) query: String?,
        model: Model,
        request: HttpServletRequest): String {
        model.addAttribute("documents", documentService.getDocumentList(query))
        model.addAttribute("query", query ?: "")
        model.addAttribute("currentPath", request.requestURI)
        return "documents"
    }

    @GetMapping("/settings")
    fun settings(model: Model, request: HttpServletRequest): String {
        model.addAttribute("config", aiConfig.get())
        model.addAttribute("currentPath", request.requestURI)
        return "settings"
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

    @PostMapping("/documents/cancel/{id}")
    @ResponseBody
    fun cancelEmbedding(@PathVariable id: UUID): ResponseEntity<Unit> {
        documentService.cancelEmbedding(id)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/documents/delete/{filename}")
    fun deleteDocument(@PathVariable() filename: String): ResponseEntity<Unit> {
        documentService.deleteDocument(filename)
        return ResponseEntity.noContent().build()
    }
}