package com.viaibot.bot.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.ActionType
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.util.concurrent.ConcurrentHashMap

@Service
class BotStatusService(
    private val telegramClient: TelegramClient
) {
    private val log = LoggerFactory.getLogger(BotStatusService::class.java)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val typingJobs = ConcurrentHashMap<Long, Job>()

    fun startTyping(chatId: Long) {
        if (typingJobs.containsKey(chatId)) return

        val job = scope.launch {
            log.debug("Starting typing indicator for chatId=$chatId")
            while (isActive) {
                sendTypingAction(chatId)
                delay(4000)
            }
        }
        typingJobs[chatId] = job
    }

    fun stopTyping(chatId: Long) {
        typingJobs.remove(chatId)?.cancel()
        log.debug("Stopped typing indicator for chatId=$chatId")
    }

    private fun sendTypingAction(chatId: Long) {
        try {
            val action = SendChatAction.builder()
                .chatId(chatId.toString())
                .action(ActionType.TYPING.name)
                .build()
            telegramClient.execute(action)
        } catch (e: TelegramApiException) {
            log.warn("Failed to send typing action for chatId=$chatId: ${e.message}")
        }
    }
}