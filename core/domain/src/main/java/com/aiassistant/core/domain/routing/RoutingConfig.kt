package com.aiassistant.core.domain.routing

object RoutingConfig {
    const val SMALL_MODEL = "llama3.2:3b"
    const val LARGE_MODEL = "qwen2.5:7b-instruct"
    const val CONFIDENCE_THRESHOLD = 0.80
    const val MIN_ANSWER_LENGTH = 20
    const val LONG_REQUEST_THRESHOLD = 400
    const val SMALL_MODEL_MAX_TOKENS = 700
    const val LARGE_MODEL_MAX_TOKENS = 1000

    val COMPLEXITY_MARKERS = listOf(
        "проанализируй архитектуру", "подробный план", "сравни несколько", "противоречив",
        "миграция", "race condition", "выбери оптимальный", "предложи стратегию"
    )

}
