package com.aiassistant.feature.chat.presentation

import com.aiassistant.core.domain.entity.AiModel
import com.aiassistant.core.domain.entity.Message

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val selectedModel: AiModel = AiModel.getDefault(),
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val systemPrompt: String = "You are a helpful AI assistant.",
    val error: String? = null,
    val currentMessage: String = ""
)