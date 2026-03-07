package com.viaibot.bot.service

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class BotAiModeService() {
    private val modeList = ConcurrentHashMap<Long, String>()

    fun setMode(chatId: Long, mode: String) {
        modeList[chatId] = mode
    }

    fun getMode(chatId: Long): String {
        return modeList[chatId] ?: "/simple"
    }
}