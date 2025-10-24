package com.viaibot.ai.entity.dto

data class AiConfigDto(
    val temperature: Double,
    val similarityThreshold: Double,
    val topK: Int
)