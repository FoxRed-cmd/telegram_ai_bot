package com.viaibot.ai.entity

data class ProcessingStatus(
    val totalPages: Int = 0,
    var processedPages: Int = 0,
    var finished: Boolean = false,
    var cancelled: Boolean = false
)