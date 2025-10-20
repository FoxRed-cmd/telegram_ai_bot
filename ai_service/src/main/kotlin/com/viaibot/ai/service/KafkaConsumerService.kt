package com.viaibot.ai.service

import com.viaibot.common.kafka.dto.AnswerMessageDto
import com.viaibot.common.kafka.dto.UserInputMessageDto
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class KafkaConsumerService(
    private val aiChatService: AiChatService,
    private val kafkaProducer: KafkaProducerService
) {
    @KafkaListener(topics = ["incoming-message"])
    fun consumeTextMessage(message: UserInputMessageDto) {
        val chatResponseText = aiChatService.chat(message)
        chatResponseText.forEach { msg -> kafkaProducer.send(AnswerMessageDto(message.chatId, msg)) }
    }
}