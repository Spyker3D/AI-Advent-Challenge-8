package com.aiassistant.core.domain.microfirst

import com.aiassistant.core.domain.inference.IncidentCategory

enum class MicroStatus { OK, UNSURE }

data class MicroCandidate(val label: IncidentCategory, val score: Double)

data class MicroClassificationResult(
    val label: IncidentCategory?,
    val score: Double,
    val margin: Double,
    val status: MicroStatus,
    val rankedCandidates: List<MicroCandidate>
)

enum class MicroFallbackReason {
    LOW_SCORE,
    LOW_MARGIN,
    EMBEDDING_ERROR,
    INVALID_VECTOR,
    PROTOTYPE_INITIALIZATION_ERROR,
    MICRO_RESULT_INVALID
}

data class MicroFirstResult(
    val finalText: String,
    val handledByMicro: Boolean,
    val fallbackUsed: Boolean,
    val microResult: MicroClassificationResult?,
    val fallbackModel: String?,
    val microLatencyMs: Long,
    val fallbackLatencyMs: Long?,
    val totalLatencyMs: Long,
    val largeLlmCalls: Int,
    val fallbackReason: MicroFallbackReason?
)
