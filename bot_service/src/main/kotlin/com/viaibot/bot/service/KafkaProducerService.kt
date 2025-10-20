package com.viaibot.bot.service

import com.viaibot.common.kafka.dto.UserInputMessageDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, UserInputMessageDto>) {

    @Value("\${spring.kafka.bot-service.topics.incoming-message.name}")
    private lateinit var incomingMsgTopic: String

    fun send(message: UserInputMessageDto) {
        kafkaTemplate.send(incomingMsgTopic, message)
    }
}