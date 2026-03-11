package com.viaibot.ai.service

import com.viaibot.ai.config.AiChatOptionsConfig
import com.viaibot.common.kafka.dto.UserInputMessageDto
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.Message
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

        private val CONTEXT_RESOLVER_PROMPT = """
            Ты — ассистент, который определяет, является ли текущий вопрос пользователя уточняющим по отношению к предыдущему диалогу.

            История диалога:
            {conversation_history}

            Текущий вопрос пользователя:
            {current_query}

            Если текущий вопрос является уточняющим (например, "расскажи подробнее", "почему", "а если", "что это" без явного указания темы),
            то сформулируй полный вопрос, объединив контекст из истории с текущим вопросом.
            
            Если это совершенно новый вопрос (не связан с предыдущей темой), просто верни его без изменений.

            Верни ТОЛЬКО итоговый вопрос, без пояснений и дополнительного текста.
            """.trimIndent()
    }

    private val simplePromptTemplate = PromptTemplate("""
            Ты — дружелюбный и полезный ассистент. Отвечай на вопросы пользователя понятно, интересно и по возможности полно.
            Используй контекст из документов как основной источник информации, но можешь дополнять своими знаниями, если это уместно.
            Старайся отвечать лаконично и четко по вопросу.

            Если в контексте нет достаточной информации, честно сообщи об этом, но постарайся помочь на основе своих знаний.

            Вопрос пользователя:
            {query}

            Контекст из документов:
            ---------------------
            {question_answer_context}
            ---------------------

            Ответ:
        """.trimIndent())

    private val strictPromptTemplate = PromptTemplate("""
            Ты — ассистент, который отвечает СТРОГО на основе предоставленного контекста.

            ИНСТРУКЦИИ:
            1. Отвечай ТОЛЬКО на основе информации из контекста ниже
            2. НЕ используй свои собственные знания
            3. Если в контексте нет ответа, скажи: "К сожалению, в загруженных документах нет информации по этому вопросу"
            4. НЕ выдумывай и НЕ домысливай информацию
            5. Цитируй факты из контекста точно

            Вопрос пользователя:
            {query}

            Контекст из документов:
            ---------------------
            {question_answer_context}
            ---------------------

            Ответ:
        """.trimIndent())

    private val customPromptTemplate = PromptTemplate("""
            {custom_prompt}

            Вопрос пользователя:
            {query}

            Контекст из документов:
            ---------------------
            {question_answer_context}
            ---------------------

            Ответ:
        """.trimIndent())

    /**
     * Генерирует уникальный ID для памяти на основе chatId и режима.
     * Это позволяет сохранять контекст диалога для каждого режима отдельно.
     */
    private fun getMemoryId(chatId: Long, mode: String): String = "chat:$chatId:mode:$mode"

    /**
     * Использует LLM для разрешения контекстных/уточняющих вопросов.
     * Если вопрос уточняющий — возвращает полный вопрос на основе истории диалога.
     * Если новый — возвращает без изменений.
     */
    private fun resolveContextualQuery(currentQuery: String, memoryId: String): String {
        try {
            val conversationHistory = chatMemory.get(memoryId)
            
            // Если история пустая — возвращаем текущий вопрос
            if (conversationHistory.isNullOrEmpty()) {
                return currentQuery
            }

            // Формируем историю диалога (последние 6 сообщений для контекста)
            val historyText = conversationHistory
                .takeLast(6)
                .filter { it.messageType.name == "USER" || it.messageType.name == "ASSISTANT" }
                .joinToString("\n") { msg: Message ->
                    "${msg.messageType.name}: ${msg.text}"
                }

            // Если история пустая — возвращаем текущий вопрос
            if (historyText.isBlank()) {
                return currentQuery
            }

            // Запрашиваем у LLM разрешение контекста
            val resolvedQuery = chatClient
                .prompt()
                .user {
                    it.param("conversation_history", historyText)
                    it.param("current_query", currentQuery)
                    it.text(CONTEXT_RESOLVER_PROMPT)
                }
                .call()
                .content()

            return resolvedQuery?.trim()?.takeIf { it.isNotBlank() } ?: currentQuery
        } catch (ex: Exception) {
            log.warn("Failed to resolve contextual query, using original: $currentQuery", ex)
            return currentQuery
        }
    }

    fun chat(message: UserInputMessageDto): List<String> {
        val config = aiConfig.get()

        log.info("AI Config: topK: {}, similarityThreshold: {}, temperature: {}, mode: {}",
            config.topK, config.similarityThreshold, config.temperature, message.mode)

        // Ограничение входного сообщения для предотвращения перегрузки
        val truncatedMessage = if (message.message.length > MAX_INPUT_LENGTH) {
            log.warn("Message truncated for chatId=${message.chatId} (original: ${message.message.length}, max: $MAX_INPUT_LENGTH)")
            message.message.take(MAX_INPUT_LENGTH)
        } else {
            message.message
        }

        // Уникальный ID памяти для каждого режима
        val memoryId = getMemoryId(message.chatId, message.mode)

        // Шаг 1: Разрешаем контекстные/уточняющие вопросы через LLM
        val resolvedQuery = resolveContextualQuery(truncatedMessage, memoryId)

        if (resolvedQuery != truncatedMessage) {
            log.info("Query resolved for chatId={}: '{}' -> '{}'", 
                message.chatId, truncatedMessage.take(50), resolvedQuery.take(50))
        }

        // Шаг 2: Поиск в векторном хранилище с разрешённым вопросом
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
            "/custom" -> {
                if (!config.customPrompt.isNullOrEmpty()) {
                    customPromptTemplate
                } else {
                    simplePromptTemplate
                }
            }
            else -> simplePromptTemplate
        }

        val questionAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(searchRequest)
            .promptTemplate(promptTemplate)
            .build()

        // Шаг 3: Генерация ответа с использованием RAG
        return try {
            val chatResponse = chatClient
                .prompt()
                .options(options)
                .advisors { a ->
                    a.param(ChatMemory.CONVERSATION_ID, memoryId)
                }
                .advisors(questionAdvisor)
                .user(resolvedQuery)
                .call()
                .chatResponse()

            val textResponse = chatResponse?.result?.output?.text ?: "Generation failed"

            if (textResponse.length > MAX_MESSAGE_LENGTH) {
                textResponse.chunked(MAX_MESSAGE_LENGTH)
            } else {
                listOf(textResponse)
            }
        } catch (ex: Exception) {
            log.error("Generation failed for chatId=${message.chatId}, mode=${message.mode}, query: $resolvedQuery", ex)
            listOf("Ошибка при генерации ответа. Попробуйте позже.")
        }
    }
}
