package com.viaibot.bot.service

import com.viaibot.common.kafka.dto.AnswerMessageDto
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.generics.TelegramClient


@Service
class KafkaConsumerService(
    private val telegramClient: TelegramClient,
    private val botStatusService: BotStatusService
) {
    private val log = LoggerFactory.getLogger(KafkaConsumerService::class.java)

    @KafkaListener(topics = ["answer-message"])
    fun consumeTextMessage(message: AnswerMessageDto) {
        try {
            botStatusService.stopTyping(message.chatId)

            val sendMessage = SendMessage
                .builder()
                .chatId(message.chatId)
                .text(message.message)
                .parseMode(ParseMode.MARKDOWN)
                .build()

            telegramClient.execute(sendMessage)
        } catch (e: Exception) {
            log.error("Failed to send message to chatId=${message.chatId}", e)
        }
    }
}