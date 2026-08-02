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
fun IncidentAction.userFacingText(): String = when (this) {
    IncidentAction.RETRY_WITH_BACKOFF -> unicode(1055, 1086, 1074, 1090, 1086, 1088, 1080, 1090, 1100, 32, 1087, 1086, 1079, 1078, 1077)
    IncidentAction.CHECK_NETWORK -> unicode(1055, 1088, 1086, 1074, 1077, 1088, 1080, 1090, 1100, 32, 1087, 1086, 1076, 1082, 1083, 1102, 1095, 1077, 1085, 1080, 1077)
    IncidentAction.RETRY_REQUEST -> unicode(1055, 1086, 1074, 1090, 1086, 1088, 1080, 1090, 1100, 32, 1079, 1072, 1087, 1088, 1086, 1089)
    IncidentAction.SHOW_EMPTY_RESPONSE_ERROR -> unicode(1055, 1088, 1086, 1074, 1077, 1088, 1080, 1090, 1100, 32, 1086, 1090, 1074, 1077, 1090)
    IncidentAction.RELOAD_LOCAL_HISTORY -> unicode(1055, 1077, 1088, 1077, 1079, 1072, 1075, 1088, 1091, 1079, 1080, 1090, 1100, 32, 1080, 1089, 1090, 1086, 1088, 1080, 1102)
    IncidentAction.REQUEST_MORE_INFORMATION -> unicode(1059, 1090, 1086, 1095, 1085, 1080, 1090, 1100, 32, 1076, 1077, 1090, 1072, 1083, 1080)
}

private fun unicode(vararg codePoints: Int): String = String(codePoints, 0, codePoints.size)
data class UserFacingIncidentResult(val title: String, val message: String, val userAction: String) {
    fun validate(expectedAction: IncidentAction) {
        val fields = listOf(title, message, userAction)
        require(fields.all { it.isNotBlank() }) { "presentation fields must not be blank" }
        require(fields.all { value -> value.any { it.code in 0x0400..0x04FF } })
        require(userAction == expectedAction.userFacingText())
        val names = IncidentCategory.entries + IncidentSeverity.entries +
            IncidentAction.entries + EvidenceState.entries + StageStatus.entries + InferenceMode.entries
        require(names.none { name -> fields.any { it.contains(name.name, ignoreCase = true) } })
    }
}
data class InferenceStageMetadata(val stage: String, val model: String, val latencyMs: Long, val promptTokens: Int?, val completionTokens: Int?, val status: StageStatus, val error: String? = null)
data class MultiStageInferenceResult(val normalizedIncident: NormalizedIncident, val decision: IncidentDecision, val presentation: UserFacingIncidentResult, val finalText: String, val stageMetadata: List<InferenceStageMetadata>, val totalLatencyMs: Long, val totalModelCalls: Int)
data class InferenceDebugMetadata(val mode: InferenceMode, val normalizedSummary: String?, val decision: IncidentDecision?, val stageMetadata: List<InferenceStageMetadata>, val totalLatencyMs: Long, val totalModelCalls: Int, val formatCompliant: Boolean)
data class InferenceExecutionResult(val finalText: String, val debugMetadata: InferenceDebugMetadata)

class InferencePipelineException(
    message: String,
    val debugMetadata: InferenceDebugMetadata,
    val normalizedIncident: NormalizedIncident? = null
) : IllegalStateException(message)
