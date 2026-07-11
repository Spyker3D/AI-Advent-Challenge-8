package com.aiassistant.core.domain.entity

data class ChatSettings(
    val provider: AiProvider = AiProvider.OPENAI,
    val selectedModel: AiModel = AiModel.getDefault(),
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val systemPrompt: String = "You are a helpful AI assistant.",
    val openAiModel: String = DEFAULT_OPENAI_MODEL,
    val localBaseUrl: String = DEFAULT_LOCAL_BASE_URL,
    val localModel: String = DEFAULT_LOCAL_MODEL,
    val localTemperature: Float = DEFAULT_LOCAL_TEMPERATURE,
    val localMaxTokens: Int = DEFAULT_LOCAL_MAX_TOKENS,
    val localContextWindow: Int = DEFAULT_LOCAL_CONTEXT_WINDOW,
    val localTopP: Float = DEFAULT_LOCAL_TOP_P,
    val localRepeatPenalty: Float = DEFAULT_LOCAL_REPEAT_PENALTY,
    val localSeed: Int? = null,
    val localSystemPrompt: String = DEFAULT_LOCAL_SYSTEM_PROMPT,
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
        const val DEFAULT_LOCAL_MODEL = "qwen2.5:7b-instruct"
        const val DEFAULT_OPENAI_MODEL = "gpt-4.1-mini"
        const val DEFAULT_LOCAL_TEMPERATURE = 0.2f
        const val DEFAULT_LOCAL_MAX_TOKENS = 700
        const val DEFAULT_LOCAL_CONTEXT_WINDOW = 8192
        const val DEFAULT_LOCAL_TOP_P = 0.9f
        const val DEFAULT_LOCAL_REPEAT_PENALTY = 1.1f
        const val DEFAULT_LOCAL_SYSTEM_PROMPT = "Ты полезный AI Assistant. Отвечай точно и понятно на языке пользователя."
        val LOCAL_CONTEXT_WINDOWS = setOf(2048, 4096, 8192, 16384)

        fun safeLocalTemperature(value: Float) = value.takeIf { it in 0f..1.5f } ?: DEFAULT_LOCAL_TEMPERATURE
        fun safeLocalMaxTokens(value: Int) = value.takeIf { it in 128..4096 } ?: DEFAULT_LOCAL_MAX_TOKENS
        fun safeLocalContextWindow(value: Int) = value.takeIf { it in LOCAL_CONTEXT_WINDOWS } ?: DEFAULT_LOCAL_CONTEXT_WINDOW
        fun safeLocalTopP(value: Float) = value.takeIf { it in 0.1f..1f } ?: DEFAULT_LOCAL_TOP_P
        fun safeLocalRepeatPenalty(value: Float) = value.takeIf { it in 0.8f..1.5f } ?: DEFAULT_LOCAL_REPEAT_PENALTY

        fun normalizeOpenAiModel(model: String?): String {
            val normalized = model?.trim().orEmpty()
            return when (normalized.lowercase()) {
                "", "gpt-4o-mini", "openai/gpt-4o-mini", "openai/gpt-4.1-mini" ->
                    DEFAULT_OPENAI_MODEL
                else -> if (normalized.startsWith("openai/", ignoreCase = true)) {
                    normalized.substringAfter('/')
                } else {
                    normalized
                }
            }
        }
        const val MIN_TEMPERATURE = 0.0f
        const val MAX_TEMPERATURE = 2.0f
        const val MIN_MAX_TOKENS = 10
        const val MAX_MAX_TOKENS = 10000
    }
}
