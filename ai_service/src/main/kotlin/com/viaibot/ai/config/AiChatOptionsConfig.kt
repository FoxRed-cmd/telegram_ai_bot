package com.viaibot.ai.config

import com.viaibot.ai.entity.dto.AiConfigDto
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AiChatOptionsConfig {
    @Value("\${spring.ai.openai.chat.options.temperature}")
    private var temperature = 0.7

    @Value("\${spring.ai.openai.chat.options.similarity-threshold}")
    private var similarityThreshold = 0.60

    @Value("\${spring.ai.openai.chat.options.top-k-value}")
    private var topK = 5

    @Value("\${spring.ai.openai.chat.options.custom-prompt}")
    private var customPrompt: String? = null

    private lateinit var config: AiConfigDto

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

        config = AiConfigDto(
            temperature,
            similarityThreshold,
            topK,
            customPrompt
        )
    }

    fun get(): AiConfigDto = config

    fun update(newConfig: AiConfigDto) {
        config = newConfig
    }
}