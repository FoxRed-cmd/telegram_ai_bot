package com.viaibot.ai.service

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class KafkaConsumerService(
    private val aiChatService: AiChatService,
    private val kafkaProducer: KafkaProducerService
) {
    @KafkaListener(topics = ["incoming-message"])
    fun consumeTextMessage(message: String) {
        val chatResponseText = aiChatService.chat(message)
        kafkaProducer.send(chatResponseText)
    }
}