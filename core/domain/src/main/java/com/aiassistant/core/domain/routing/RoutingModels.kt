package com.aiassistant.core.domain.routing

import com.aiassistant.core.domain.entity.ContextStrategy

enum class Ambiguity { LOW, MEDIUM, HIGH }

data class SmallModelDecision(
    val answer: String,
    val confidence: Double,
    val needsEscalation: Boolean,
    val ambiguity: Ambiguity,
    val sufficientContext: Boolean,
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
    val completionTokens: Int? = null,
    val parseFailure: RoutingParseFailure? = null
)

data class RoutingDebugMetadata(
    val routingEnabled: Boolean,
    val firstModel: String?,
    val finalModel: String,
    val escalated: Boolean,
    val confidence: Double?,
    val reason: RoutingReason?,
    val smallLatencyMs: Long?,
    val largeLatencyMs: Long?,
    val totalLatencyMs: Long?,
    val contextStrategy: ContextStrategy? = null,
    val parseFailure: RoutingParseFailure? = null,
    val structuredFormatEnabled: Boolean = false
)

enum class RoutingParseFailure { MISSING_QUOTES, EMPTY_REASON, TYPE_MISMATCH, OTHER }

enum class RoutingReason {
    COMPLEX_REQUEST, LONG_REQUEST, LOW_CONFIDENCE, MODEL_REQUESTED_ESCALATION,
    ANSWER_TOO_SHORT, INVALID_JSON, SMALL_MODEL_ERROR, LARGE_MODEL_ERROR
}
