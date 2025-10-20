package com.viaibot.ai.service

import com.viaibot.common.kafka.dto.AnswerMessageDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, AnswerMessageDto>) {
    @Value("\${spring.kafka.ai-service.topics.answer-message.name}")
    private lateinit var answerMsgTopic: String

    fun send(message: AnswerMessageDto) {
        kafkaTemplate.send(answerMsgTopic, message)
    }
}