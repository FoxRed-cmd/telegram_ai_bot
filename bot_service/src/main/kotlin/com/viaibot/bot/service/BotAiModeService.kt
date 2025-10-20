package com.viaibot.bot.service

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.generics.TelegramClient

@Service
class BotAiModeService() {
    private val modeList = HashMap<Long, String>()

    fun setMode(chatId: Long, mode: String) {
        modeList[chatId] = mode
    }

    fun getMode(chatId: Long): String {
        return modeList[chatId] ?: "/simple"
    }
}