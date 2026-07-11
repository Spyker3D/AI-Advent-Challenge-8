package com.aiassistant.core.domain.entity

enum class AiModel(
    val modelName: String,
    val displayName: String
) {
    GPT_4_1_MINI(
        "gpt-4.1-mini",
        "GPT-4.1 Mini"
    ),
    LLAMA_3_2_1B(
        "meta-llama/llama-3.2-1b-instruct",
        "Llama 3.2 1B"
    ),
    QWEN_3_32B(
        "qwen/qwen3-32b",
        "Qwen3 32B"
    ),
    DEEPSEEK_R1(
        "deepseek/deepseek-r1",
        "DeepSeek R1"
    );

    companion object {
        fun fromModelName(modelName: String): AiModel? {
            return values().find { it.modelName == modelName }
        }

        fun getDefault(): AiModel = GPT_4_1_MINI
    }
}
