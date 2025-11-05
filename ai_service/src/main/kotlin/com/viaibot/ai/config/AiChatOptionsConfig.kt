package com.viaibot.ai.config

import com.viaibot.ai.entity.dto.AiConfigDto
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class AiChatOptionsConfig {
    @Value("\${spring.ai.ollama.chat.options.temperature}")
    private var temperature = 0.7

    @Value("\${spring.ai.ollama.chat.options.similarity-threshold}")
    private var similarityThreshold = 0.60

    @Value("\${spring.ai.ollama.chat.options.top-k-value}")
    private var topK = 5

    @Value("\${spring.ai.ollama.chat.options.custom-prompt}")
    private var customPrompt: String? = null

    private lateinit var configRef: AtomicReference<AiConfigDto>

    @PostConstruct
    fun init() {
        customPrompt = """
            Ты персональный помощник. Отвечай на вопросы пользователя строго по информации из контекста.
            Если в контексте нет ответа на вопрос, сообщи об этом пользователю и предложи несколько вариантов того,
            чтобы пользователь мог спросить исходя из контекста. 

            Вопрос пользователя:
            {query}

            Контекст заключен в ---------------

            ---------------
            {question_answer_context}
            ---------------
        """.trimIndent()

        configRef = AtomicReference(
            AiConfigDto(
                temperature = temperature,
                similarityThreshold = similarityThreshold,
                topK = topK,
                customPrompt = customPrompt
            )
        )
    }

    fun get(): AiConfigDto = configRef.get()

    fun update(newConfig: AiConfigDto, trueUpdate: Boolean = true) {
        newConfig.isUpdate = trueUpdate
        configRef.set(newConfig)
    }
}