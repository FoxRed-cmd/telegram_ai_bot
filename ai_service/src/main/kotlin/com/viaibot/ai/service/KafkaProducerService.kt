package com.viaibot.ai.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, String>) {
    @Value("\${spring.kafka.ai-service.topics.answer-message.name}")
    private lateinit var answerMsgTopic: String

    fun send(message: String) {
        kafkaTemplate.send(answerMsgTopic, message)
    }
}