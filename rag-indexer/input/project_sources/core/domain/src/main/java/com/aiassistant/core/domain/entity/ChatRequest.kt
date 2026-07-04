package com.aiassistant.core.domain.entity

data class ChatRequest(
    val message: String,
    val model: AiModel,
    val temperature: Float,
    val maxTokens: Int,
    val systemPrompt: String? = null,
    val history: List<Message> = emptyList()
)