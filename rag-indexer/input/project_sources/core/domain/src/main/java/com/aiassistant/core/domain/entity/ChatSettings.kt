package com.aiassistant.core.domain.entity

data class ChatSettings(
    val selectedModel: AiModel = AiModel.getDefault(),
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val systemPrompt: String = "You are a helpful AI assistant.",
    // Day 2 fields
    val useJsonFormat: Boolean = false,
    val limitLength: Boolean = false,
    val useStopSequence: Boolean = false,
    val stopSequenceText: String = "",
    // Context compression fields
    val useContextCompression: Boolean = false,
    val keepLastMessagesCount: Int = 6,
    // Context strategy field
    val contextStrategy: ContextStrategy = ContextStrategy.SLIDING_WINDOW
) {
    companion object {
        const val MIN_TEMPERATURE = 0.0f
        const val MAX_TEMPERATURE = 2.0f
        const val MIN_MAX_TOKENS = 10
        const val MAX_MAX_TOKENS = 10000
    }
}
