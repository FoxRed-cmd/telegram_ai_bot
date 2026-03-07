package com.viaibot.ai.entity

data class ProcessingStatus(
    val totalPages: Int = 0,
    var processedPages: Int = 0,
    var finished: Boolean = false,
    var cancelled: Boolean = false,
    var failed: Boolean = false,
    var totalChunks: Int = 0,
    var embeddedChunks: Int = 0,
    var errorMessage: String? = null
) {
    val progressPercent: Int
        get() = when {
            failed -> 100
            cancelled -> 0
            totalChunks > 0 -> (embeddedChunks * 100 / totalChunks)
            totalPages > 0 -> (processedPages * 100 / totalPages)
            else -> 0
        }
}