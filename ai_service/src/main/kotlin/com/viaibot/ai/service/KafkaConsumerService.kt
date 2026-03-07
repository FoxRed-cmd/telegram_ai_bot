package com.viaibot.ai.service

import com.viaibot.common.kafka.dto.AnswerMessageDto
import com.viaibot.common.kafka.dto.UserInputMessageDto
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class KafkaConsumerService(
    private val aiChatService: AiChatService,
    private val kafkaProducer: KafkaProducerService
) {
    private val log = LoggerFactory.getLogger(KafkaConsumerService::class.java)

    @KafkaListener(topics = ["incoming-message"])
    fun consumeTextMessage(message: UserInputMessageDto) {
        try {
            val chatResponseText = aiChatService.chat(message)
            chatResponseText.forEach { msg ->
                kafkaProducer.send(AnswerMessageDto(message.chatId, msg))
            }
        } catch (e: Exception) {
            log.error("Failed to process message for chatId=${message.chatId}", e)
            kafkaProducer.send(
                AnswerMessageDto(
                    message.chatId,
                    "Произошла ошибка при обработке запроса. Попробуйте позже."
                )
            )
        }
    }
}