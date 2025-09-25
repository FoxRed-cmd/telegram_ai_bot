package com.viaibot.ai.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class AiChatService(private val chatClient: ChatClient) {

    public fun chat(question: String): String {
        val chatResponse = chatClient
            .prompt()
            .user(question)
            .call()
            .chatResponse()

        return chatResponse?.result?.output?.text ?: "Generation failed"
    }
}