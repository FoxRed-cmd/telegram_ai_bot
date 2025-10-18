package com.viaibot.ai.controller

import com.viaibot.ai.entity.dto.UserInputDto
import com.viaibot.common.kafka.dto.UserInputMessageDto
import com.viaibot.ai.service.AiChatService
import com.viaibot.ai.service.AiEmbeddingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api")
class AiController(
    private val aiChatService: AiChatService,
    private val aiEmbeddingService: AiEmbeddingService
) {

    @PostMapping("/chat")
    fun GetHelloWorld(@RequestBody userInput: UserInputDto): ResponseEntity<String> {
        return ResponseEntity.ok(aiChatService.chat(UserInputMessageDto(1, userInput.userMessage)))
    }

    @PostMapping("/upload")
    fun uploadDocument(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        aiEmbeddingService.embed(file.resource)
        return ResponseEntity.ok("The file has been successfully uploaded")
    }
}