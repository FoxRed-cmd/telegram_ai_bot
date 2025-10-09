package com.viaibot.bot.service

import com.viaibot.common.kafka.dto.UserInputMessageDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update

@Service
class BotService(
    private val kafkaProducerService: KafkaProducerService,
    private val botStatusService: BotStatusService
) : SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    @Value("\${telegram.bot.token}")
    private lateinit var botToken: String

    private val log = LoggerFactory.getLogger(BotService::class.java)

    override fun getBotToken(): String {
        return botToken
    }

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer {
        return this
    }

    override fun consume(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            log.info("Incoming message - id: {}, text: {}", update.message.chatId, update.message.text)

            botStatusService.startTyping(update.message.chatId)

            kafkaProducerService.send(UserInputMessageDto(update.message.chatId, update.message.text))
        }
    }
}