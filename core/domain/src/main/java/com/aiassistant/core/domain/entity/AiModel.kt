package com.aiassistant.core.domain.entity

enum class AiModel(val modelName: String, val displayName: String) {
    GPT_4O_MINI("openai/gpt-4o-mini", "GPT-4o Mini"),
    DEEPSEEK_CHAT("deepseek/deepseek-chat", "DeepSeek Chat"),
    KIMI_K2("moonshotai/kimi-k2", "Kimi K2");

    companion object {
        fun fromModelName(modelName: String): AiModel? {
            return values().find { it.modelName == modelName }
        }

        fun getDefault(): AiModel = GPT_4O_MINI
    }
}