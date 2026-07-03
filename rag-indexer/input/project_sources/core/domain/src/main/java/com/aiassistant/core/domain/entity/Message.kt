package com.aiassistant.core.domain.entity

data class Message(
    val id: String,
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: AiResponseMetadata? = null,
    val tokenMetrics: TokenMetrics? = null
)

enum class MessageRole(val value: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system")
}