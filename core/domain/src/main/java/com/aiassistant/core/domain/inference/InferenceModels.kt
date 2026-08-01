package com.aiassistant.core.domain.inference

enum class InferenceMode { MONOLITHIC, MULTI_STAGE }
enum class IncidentCategory { NETWORK_UNAVAILABLE, OPENAI_RATE_LIMIT, OPENAI_TIMEOUT, EMPTY_AI_RESPONSE, LOCAL_HISTORY_UNAVAILABLE, AMBIGUOUS }
enum class IncidentSeverity { LOW, MEDIUM, HIGH }
enum class IncidentAction { CHECK_NETWORK, RETRY_WITH_BACKOFF, RETRY_REQUEST, SHOW_EMPTY_RESPONSE_ERROR, RELOAD_LOCAL_HISTORY, REQUEST_MORE_INFORMATION }
enum class StageStatus { OK, FORMAT_ERROR, MODEL_ERROR, VALIDATION_ERROR, SKIPPED }
enum class EvidenceState { SUPPORTED, CONFLICTING, INSUFFICIENT }

data class NormalizedIncident(
    val observedFacts: List<String>,
    val normalizedSummary: String
)
data class IncidentDecision(
    val category: IncidentCategory,
    val severity: IncidentSeverity,
    val action: IncidentAction,
    val confidence: Double,
    val evidenceState: EvidenceState,
    val supportingEvidence: List<String>,
    val contradictingEvidence: List<String>
) {
    fun validate() {
        require(confidence in 0.0..1.0) { "confidence must be between 0 and 1" }
        require(action == category.requiredAction()) { "action must match category" }
        when (evidenceState) {
            EvidenceState.SUPPORTED -> {
                require(supportingEvidence.isNotEmpty()) { "supported decision requires supporting evidence" }
                require(contradictingEvidence.isEmpty()) { "supported decision must not contain contradictions" }
                require(category != IncidentCategory.AMBIGUOUS) { "supported decision requires a specific category" }
            }
            EvidenceState.CONFLICTING -> {
                require(supportingEvidence.isNotEmpty()) { "conflicting evidence requires supporting evidence" }
                require(contradictingEvidence.isNotEmpty()) { "conflicting evidence requires contradictions" }
                require(category == IncidentCategory.AMBIGUOUS) { "conflicting evidence requires AMBIGUOUS" }
            }
            EvidenceState.INSUFFICIENT -> {
                require(category == IncidentCategory.AMBIGUOUS) { "insufficient evidence requires AMBIGUOUS" }
            }
        }
        require(category == IncidentCategory.AMBIGUOUS || contradictingEvidence.isEmpty()) {
            "a specific category must not have contradicting evidence"
        }
    }
}

fun IncidentCategory.requiredAction(): IncidentAction = when (this) {
    IncidentCategory.NETWORK_UNAVAILABLE -> IncidentAction.CHECK_NETWORK
    IncidentCategory.OPENAI_RATE_LIMIT -> IncidentAction.RETRY_WITH_BACKOFF
    IncidentCategory.OPENAI_TIMEOUT -> IncidentAction.RETRY_REQUEST
    IncidentCategory.EMPTY_AI_RESPONSE -> IncidentAction.SHOW_EMPTY_RESPONSE_ERROR
    IncidentCategory.LOCAL_HISTORY_UNAVAILABLE -> IncidentAction.RELOAD_LOCAL_HISTORY
    IncidentCategory.AMBIGUOUS -> IncidentAction.REQUEST_MORE_INFORMATION
}
data class UserFacingIncidentResult(val title: String, val message: String, val userAction: String)
data class InferenceStageMetadata(val stage: String, val model: String, val latencyMs: Long, val promptTokens: Int?, val completionTokens: Int?, val status: StageStatus, val error: String? = null)
data class MultiStageInferenceResult(val normalizedIncident: NormalizedIncident, val decision: IncidentDecision, val presentation: UserFacingIncidentResult, val finalText: String, val stageMetadata: List<InferenceStageMetadata>, val totalLatencyMs: Long, val totalModelCalls: Int)
data class InferenceDebugMetadata(val mode: InferenceMode, val normalizedSummary: String?, val decision: IncidentDecision?, val stageMetadata: List<InferenceStageMetadata>, val totalLatencyMs: Long, val totalModelCalls: Int, val formatCompliant: Boolean)
data class InferenceExecutionResult(val finalText: String, val debugMetadata: InferenceDebugMetadata)

class InferencePipelineException(
    message: String,
    val debugMetadata: InferenceDebugMetadata,
    val normalizedIncident: NormalizedIncident? = null
) : IllegalStateException(message)
