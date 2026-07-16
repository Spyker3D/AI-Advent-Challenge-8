package com.aiassistant.core.domain.entity

data class LocalGenerationMetrics(
    val model: String,
    val temperature: Float,
    val maxOutputTokens: Int,
    val contextWindow: Int,
    val topP: Float,
    val repeatPenalty: Float,
    val seed: Int?,
    val promptTokens: Int?,
    val outputTokens: Int?,
    val totalDurationNanos: Long?,
    val loadDurationNanos: Long?,
    val evalDurationNanos: Long?
) {
    val generationSeconds: Double? get() = totalDurationNanos?.div(1_000_000_000.0)
    val loadSeconds: Double? get() = loadDurationNanos?.div(1_000_000_000.0)
    val tokensPerSecond: Double? get() = if (outputTokens != null && evalDurationNanos != null && evalDurationNanos > 0) {
        outputTokens * 1_000_000_000.0 / evalDurationNanos
    } else null
}
