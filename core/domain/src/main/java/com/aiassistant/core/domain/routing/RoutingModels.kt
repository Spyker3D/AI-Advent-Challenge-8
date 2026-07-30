package com.aiassistant.core.domain.routing

data class SmallModelDecision(
    val answer: String,
    val confidence: Double,
    val needsEscalation: Boolean,
    val reason: String
)

data class RoutingResult(
    val answer: String,
    val finalModel: String,
    val escalated: Boolean,
    val smallModelConfidence: Double?,
    val escalationReason: RoutingReason?,
    val smallModelLatencyMs: Long?,
    val largeModelLatencyMs: Long?,
    val totalLatencyMs: Long,
    val smallModelRawResponse: String? = null
)

enum class RoutingReason {
    COMPLEX_REQUEST,
    LONG_REQUEST,
    LOW_CONFIDENCE,
    MODEL_REQUESTED_ESCALATION,
    ANSWER_TOO_SHORT,
    INVALID_JSON,
    SMALL_MODEL_ERROR,
    LARGE_MODEL_ERROR
}
