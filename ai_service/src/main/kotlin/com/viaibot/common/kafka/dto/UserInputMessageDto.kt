package com.viaibot.common.kafka.dto

data class UserInputMessageDto(
    val chatId: Long,
    val message: String,
    val mode: String
)