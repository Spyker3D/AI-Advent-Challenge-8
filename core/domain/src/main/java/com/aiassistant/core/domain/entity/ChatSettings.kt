package com.aiassistant.core.domain.entity

data class ChatSettings(
    val provider: AiProvider = AiProvider.OPENROUTER,
    val selectedModel: AiModel = AiModel.getDefault(),
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val systemPrompt: String = "You are a helpful AI assistant.",
    val localBaseUrl: String = DEFAULT_LOCAL_BASE_URL,
    val localModel: String = DEFAULT_LOCAL_MODEL,
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
        const val DEFAULT_LOCAL_BASE_URL = "http://10.0.2.2:11434"
        const val DEFAULT_LOCAL_MODEL = "llama3.2:3b"
        const val MIN_TEMPERATURE = 0.0f
        const val MAX_TEMPERATURE = 2.0f
        const val MIN_MAX_TOKENS = 10
        const val MAX_MAX_TOKENS = 10000
    }
}
