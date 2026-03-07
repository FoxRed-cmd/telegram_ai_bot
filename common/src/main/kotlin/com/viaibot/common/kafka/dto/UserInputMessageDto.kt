package com.viaibot.common.kafka.dto

import java.io.Serializable

data class UserInputMessageDto(
    val chatId: Long,
    val message: String,
    val mode: String
) : Serializable
