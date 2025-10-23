package com.viaibot.ai.config

import com.viaibot.ai.entity.dto.AiConfigDto
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class AiChatOptionsConfig {
    @Value("\${spring.ai.openai.chat.options.temperature}")
    private var temperature = 0.0

    private lateinit var configRef: AtomicReference<AiConfigDto>

    @PostConstruct
    fun init() {
        configRef = AtomicReference(
            AiConfigDto(temperature = temperature)
        )
    }

    fun get(): AiConfigDto = configRef.get()

    fun update(newConfig: AiConfigDto) {
        configRef.set(newConfig)
    }
}