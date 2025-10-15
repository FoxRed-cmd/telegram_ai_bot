package com.viaibot.ai.service

import com.viaibot.common.kafka.dto.UserInputMessageDto
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class AiChatService(
    private val chatClient: ChatClient,
    private val vectorStore: VectorStore
) {
    private val promptTemplate = PromptTemplate("""
            Ты — дружелюбный и полезный ассистент. Отвечай на вопросы пользователя понятно, интересно и по возможности полно. 
            Не ограничивайся никакими документами, если хочешь можешь использовать свои знания. Старайся отвечать лаконично и четко по вопросу.
            
            {query}
            
            Контекстная информация указана ниже, обрамленная ---------------------

            ---------------------
            {question_answer_context}
            ---------------------
        """.trimIndent())

    private val searchRequest = SearchRequest.builder()
        .similarityThreshold(0.7)
        .topK(5)
        .build()

    private val questionAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
        .searchRequest(searchRequest)
        .promptTemplate(promptTemplate)
        .build()

    fun chat(message: UserInputMessageDto): String {
        val chatResponse = chatClient
            .prompt()
            .advisors(questionAdvisor)
            .user(message.message)
            .call()
            .chatResponse()

        return chatResponse?.result?.output?.text ?: "Generation failed"
    }
}