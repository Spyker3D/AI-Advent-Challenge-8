package com.aiassistant.core.domain.entity

data class AiChatResponse(
    val message: String,
    val metadata: AiResponseMetadata?
)
