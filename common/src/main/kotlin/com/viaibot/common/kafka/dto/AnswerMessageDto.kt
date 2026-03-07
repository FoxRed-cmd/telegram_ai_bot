package com.viaibot.common.kafka.dto

import java.io.Serializable

data class AnswerMessageDto(
    val chatId: Long,
    val message: String
) : Serializable
