package com.viaibot.ai.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory

@Configuration
class AiChatConfig {
    @Bean
    fun chatClient(chatClientBuilder: ChatClient.Builder, chatMemory: ChatMemory): ChatClient {
        return chatClientBuilder
            .defaultAdvisors(MessageChatMemoryAdvisor
                .builder(chatMemory)
                .build()
            )
            .build()
    }
}