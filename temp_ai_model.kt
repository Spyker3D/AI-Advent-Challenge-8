package com.aiassistant.core.domain.entity

enum class AiModel(
    val modelName: String,
    val displayName: String,
    val inputPricePerMillionTokensUsd: Double?,
    val outputPricePerMillionTokensUsd: Double?
) {
    // Prices are approximate and should be verified on OpenRouter before final reporting.
    CHATGPT_4O_MINI(
        "openai/gpt-4o-mini",
        "ChatGPT 4o Mini",
        0.15, // $0.15 per 1M input tokens
        0.60  // $0.60 per 1M output tokens
    ),
    LLAMA_3_2_1B(
        "meta-llama/llama-3.2-1b-instruct",
        "Llama 3.2 1B",
        0.10, // Approximate pricing
        0.10  // Approximate pricing
    ),
    QWEN_3_32B(
        "qwen/qwen3-32b",
        "Qwen3 32B",
        0.80, // Approximate pricing
        1.20  // Approximate pricing
    ),
    DEEPSEEK_R1(
        "deepseek/deepseek-r1",
        "DeepSeek R1",
        0.50, // Approximate pricing
        0.50  // Approximate pricing
    );

    companion object {
        fun fromModelName(modelName: String): AiModel? {
            return values().find { it.modelName == modelName }
        }

        fun getDefault(): AiModel = CHATGPT_4O_MINI
    }
}