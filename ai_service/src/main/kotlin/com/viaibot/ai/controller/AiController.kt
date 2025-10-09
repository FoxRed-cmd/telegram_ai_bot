package com.viaibot.ai.controller

import com.viaibot.ai.entity.dto.UserInputDto
import com.viaibot.common.kafka.dto.UserInputMessageDto
import com.viaibot.ai.service.AiChatService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class AiController(private val aiChatService: AiChatService) {

    @PostMapping("/chat")
    fun GetHelloWorld(@RequestBody userInput: UserInputDto): ResponseEntity<String> {
        return ResponseEntity.ok(aiChatService.chat(UserInputMessageDto(1, userInput.userMessage)))
    }
}