package com.aiassistant.core.domain.entity

data class ChatSettings(
    val selectedModel: AiModel = AiModel.getDefault(),
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val systemPrompt: String = "You are a helpful AI assistant."
) {
    companion object {
        const val MIN_TEMPERATURE = 0.0f
        const val MAX_TEMPERATURE = 2.0f
        const val MIN_MAX_TOKENS = 10
        const val MAX_MAX_TOKENS = 4000
    }
}