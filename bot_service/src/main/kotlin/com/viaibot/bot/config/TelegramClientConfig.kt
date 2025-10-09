package com.viaibot.bot.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.generics.TelegramClient

@Configuration
class TelegramClientConfig {

    @Value("\${telegram.bot.token}")
    private lateinit var botToken: String

    @Bean
    fun telegramClient(): TelegramClient {
        return OkHttpTelegramClient(botToken)
    }
}