package com.viaibot.ai.service

import com.viaibot.common.kafka.dto.UserInputMessageDto
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class AiChatService(private val chatClient: ChatClient) {

    fun chat(message: UserInputMessageDto): String {
        val chatResponse = chatClient
            .prompt()
            .user(message.message)
            .call()
            .chatResponse()

        return chatResponse?.result?.output?.text ?: "Generation failed"
    }
}