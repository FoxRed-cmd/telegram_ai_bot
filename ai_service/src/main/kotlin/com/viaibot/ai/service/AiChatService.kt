package com.viaibot.ai.service

import com.viaibot.ai.config.AiChatOptionsConfig
import com.viaibot.common.kafka.dto.UserInputMessageDto
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AiChatService(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory,
    private val vectorStore: VectorStore,
    private val aiConfig: AiChatOptionsConfig
) {

    @Value("\${spring.ai.ollama.chat.options.simple-prompt}")
    private var simplePrompt: String? = null

    @Value("\${spring.ai.ollama.chat.options.strict-prompt}")
    private var strictPrompt: String? = null

    lateinit var simplePromptTemplate: PromptTemplate

    lateinit var strictPromptTemplate: PromptTemplate

    lateinit var searchRequest: SearchRequest

    lateinit var questionAdvisor: QuestionAnswerAdvisor

    private val log = LoggerFactory.getLogger(AiChatService::class.java)

    @PostConstruct
    fun init() {
        simplePromptTemplate = PromptTemplate(if (!simplePrompt.isNullOrEmpty()) simplePrompt else """
            Ты — дружелюбный и полезный ассистент. Отвечай на вопросы пользователя понятно, интересно и по возможности полно. 
            Не ограничивайся никакими документами, если хочешь можешь использовать свои знания. Старайся отвечать лаконично и четко по вопросу.
            
            {query}
            
            Контекстная информация указана ниже, обрамленная ---------------------

            ---------------------
            {question_answer_context}
            ---------------------
        """.trimIndent())

        strictPromptTemplate = PromptTemplate(if (!strictPrompt.isNullOrEmpty()) strictPrompt else """
            {query}

            Контекстная информация указана ниже, обрамленная ---------------------

            ---------------------
            {question_answer_context}
            ---------------------

            Учитывая контекст и предоставленную информацию об истории вопроса, а не предыдущие знания,
            ответь на комментарий пользователя. Если ответ не соответствует контексту, сообщи
            пользователю, что ты не можешь ответить на вопрос.
        """.trimIndent())
    }

    fun chat(message: UserInputMessageDto): List<String> {
        /*val memory = chatMemory.get(message.chatId.toString())
        val recentUserMessages = memory
            .filter { it -> it.messageType == MessageType.USER }
            .takeLast(1)
            .joinToString("\n") { it.text }
        val searchQuery = "$recentUserMessages\n${message.message}"*/
        val config = aiConfig.get()

        log.info("AI Config: topK: {}, similarityThreshold: {}, temperature: {}",
            config.topK, config.similarityThreshold, config.temperature)

        searchRequest = SearchRequest.builder()
            .similarityThreshold(config.similarityThreshold)
            .topK(config.topK)
            .build()

        questionAdvisor = buildQuestionAdvisor(message.mode)

        val options = OllamaOptions.builder()
            .temperature(config.temperature)
            .build()

        val chatResponse = chatClient
            .prompt()
            .options(options)
            .advisors { a -> a.param(ChatMemory.CONVERSATION_ID, message.chatId) }
            .advisors(questionAdvisor)
            .user(message.message)
            .call()
            .chatResponse()

        val textResponse = chatResponse?.result?.output?.text ?: "Generation failed"
        if (textResponse.length > MAX_MESSAGE_LENGTH) {
            return textResponse.chunked(MAX_MESSAGE_LENGTH)
        }

        return listOf(textResponse)
    }

    private fun buildQuestionAdvisor(mode: String): QuestionAnswerAdvisor {
        return when(mode) {
            "/simple" -> QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .promptTemplate(simplePromptTemplate)
                .build()
            "/strict" -> QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .promptTemplate(strictPromptTemplate)
                .build()
            else ->  QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .promptTemplate(simplePromptTemplate)
                .build()
        }
    }
}

const val MAX_MESSAGE_LENGTH = 4096
