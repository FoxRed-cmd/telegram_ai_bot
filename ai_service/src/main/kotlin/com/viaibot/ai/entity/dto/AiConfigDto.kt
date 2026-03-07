package com.viaibot.ai.entity.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class AiConfigDto(
    @field:Min(0, message = "Temperature must be at least 0")
    @field:Max(1, message = "Temperature must be at most 1")
    val temperature: Double,

    @field:Min(0, message = "Similarity threshold must be at least 0")
    @field:Max(1, message = "Similarity threshold must be at most 1")
    val similarityThreshold: Double,

    @field:Min(1, message = "Top K must be at least 1")
    val topK: Int,

    @field:Size(max = 10000, message = "Custom prompt must not exceed 10000 characters")
    val customPrompt: String?
)