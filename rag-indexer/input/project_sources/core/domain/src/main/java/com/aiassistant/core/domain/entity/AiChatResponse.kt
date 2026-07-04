package com.aiassistant.core.domain.entity

data class AiChatResponse(
    val message: String,
    val metadata: AiResponseMetadata?,
    val tokenMetrics: TokenMetrics? = null
)
