package com.viaibot.ai.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ai.chat.client.ChatClient;

@Configuration
class AiChatConfig {
    @Bean
    fun chatClient(chatClientBuilder: ChatClient.Builder): ChatClient {
        return chatClientBuilder.build()
    }
}