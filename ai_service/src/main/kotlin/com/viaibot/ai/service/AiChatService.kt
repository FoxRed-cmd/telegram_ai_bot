package com.viaibot.ai.service

import com.viaibot.ai.config.AiChatOptionsConfig
import com.viaibot.common.kafka.dto.UserInputMessageDto
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class AiChatService(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory,
    private val vectorStore: VectorStore,
    private val aiConfig: AiChatOptionsConfig
) {
    private val log = LoggerFactory.getLogger(AiChatService::class.java)

    companion object {
        private const val MAX_MESSAGE_LENGTH = 4096
        private const val MAX_INPUT_LENGTH = 4000
    }

    private val simplePromptTemplate = PromptTemplate("""
            Ты — дружелюбный и полезный ассистент. Отвечай на вопросы пользователя понятно, интересно и по возможности полно.
            Не ограничивайся никакими документами, если хочешь можешь использовать свои знания. Старайся отвечать лаконично и четко по вопросу.

            {query}

            Контекстная информация указана ниже, обрамленная ---------------------

            ---------------------
            {question_answer_context}
            ---------------------
        """.trimIndent())

    private val strictPromptTemplate = PromptTemplate("""
            {query}

            Контекстная информация указана ниже, обрамленная ---------------------

            ---------------------
            {question_answer_context}
            ---------------------

            Учитывая контекст и предоставленную информацию об истории вопроса, а не предыдущие знания,
            ответь на комментарий пользователя. Если ответ не соответствует контексту, сообщи
            пользователю, что ты не можешь ответить на вопрос.
        """.trimIndent())

    /**
     * Генерирует уникальный ID для памяти на основе chatId и режима.
     * Это позволяет сохранять контекст диалога для каждого режима отдельно.
     */
    private fun getMemoryId(chatId: Long, mode: String): String = "chat:$chatId:mode:$mode"

    fun chat(message: UserInputMessageDto): List<String> {
        val config = aiConfig.get()

        log.info("AI Config: topK: {}, similarityThreshold: {}, temperature: {}",
            config.topK, config.similarityThreshold, config.temperature)

        // Ограничение входного сообщения для предотвращения перегрузки
        val truncatedMessage = if (message.message.length > MAX_INPUT_LENGTH) {
            log.warn("Message truncated for chatId=${message.chatId} (original: ${message.message.length}, max: $MAX_INPUT_LENGTH)")
            message.message.take(MAX_INPUT_LENGTH)
        } else {
            message.message
        }

        val searchRequest = SearchRequest.builder()
            .similarityThreshold(config.similarityThreshold)
            .topK(config.topK)
            .build()

        val options = OpenAiChatOptions.builder()
            .temperature(config.temperature)
            .build()

        val promptTemplate = when (message.mode) {
            "/simple" -> simplePromptTemplate
            "/strict" -> strictPromptTemplate
            "/custom" -> if (!config.customPrompt.isNullOrEmpty())
                PromptTemplate(config.customPrompt)
            else simplePromptTemplate
            else -> simplePromptTemplate
        }

        val questionAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(searchRequest)
            .promptTemplate(promptTemplate)
            .build()

        // Уникальный ID памяти для каждого режима
        val memoryId = getMemoryId(message.chatId, message.mode)

        return try {
            val chatResponse = chatClient
                .prompt()
                .options(options)
                .advisors { a ->
                    a.param(ChatMemory.CONVERSATION_ID, memoryId)
                }
                .advisors(questionAdvisor)
                .user(truncatedMessage)
                .call()
                .chatResponse()

            val textResponse = chatResponse?.result?.output?.text ?: "Generation failed"

            if (textResponse.length > MAX_MESSAGE_LENGTH) {
                textResponse.chunked(MAX_MESSAGE_LENGTH)
            } else {
                listOf(textResponse)
            }
        } catch (ex: Exception) {
            log.error("Generation failed for chatId=${message.chatId}, mode=${message.mode}", ex)
            listOf("Ошибка при генерации ответа. Попробуйте позже.")
        }
    }
}
