package com.viaibot.bot.service

import com.viaibot.common.kafka.dto.UserInputMessageDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.generics.TelegramClient

@Service
class BotService(
    private val kafkaProducerService: KafkaProducerService,
    private val botStatusService: BotStatusService,
    private val telegramClient: TelegramClient,
    private val botAiModeService: BotAiModeService
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

            val message = update.message

            executeCommand(message.chatId, message.text)
        }
    }

    fun sendMessage(text: String, chatId: Long) {
        botStatusService.stopTyping(chatId)

        val message = SendMessage
            .builder()
            .chatId(chatId)
            .text(text)
            .build()

        telegramClient.execute(message)
    }

    private fun executeCommand(chatId: Long, text: String) {
        botStatusService.startTyping(chatId)
        when(text) {
            "/start", "/changemode" -> setKeyboard(chatId)
            "/simple", "/strict" -> {
                botAiModeService.setMode(chatId, text)
                sendMessage("Выбран режим ${text.replace("/", "")}", chatId)
            }
            else -> kafkaProducerService.send(UserInputMessageDto(chatId, text, botAiModeService.getMode(chatId)))
        }
    }

    private fun setKeyboard(chatId: Long) {
        botStatusService.stopTyping(chatId)

        val keyboard = ReplyKeyboardMarkup.builder()
            .keyboard(listOf(KeyboardRow("/simple", "/strict")))
            .resizeKeyboard(true)
            .isPersistent(true)
            .oneTimeKeyboard(true)
            .build()

        val message = SendMessage
            .builder()
            .chatId(chatId)
            .text("""
                Выберите режим работы:
                simple (простой) - бот будет отвечать опираясь не только на данные из документов, но и на свои собственные знания
                strict (строгий) - бот будет отвечать опираясь только на данные из документов
            """.trimIndent())
            .replyMarkup(keyboard)
            .build()

        telegramClient.execute(message)
    }
}