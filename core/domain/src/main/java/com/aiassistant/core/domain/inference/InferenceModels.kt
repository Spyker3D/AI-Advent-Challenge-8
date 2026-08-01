package com.aiassistant.core.domain.inference

enum class InferenceMode { MONOLITHIC, MULTI_STAGE }
enum class IncidentCategory { NETWORK_UNAVAILABLE, OPENAI_RATE_LIMIT, OPENAI_TIMEOUT, EMPTY_AI_RESPONSE, LOCAL_HISTORY_UNAVAILABLE, AMBIGUOUS }
enum class IncidentSeverity { LOW, MEDIUM, HIGH }
enum class IncidentAction { CHECK_NETWORK, RETRY_WITH_BACKOFF, RETRY_REQUEST, SHOW_EMPTY_RESPONSE_ERROR, RELOAD_LOCAL_HISTORY, REQUEST_MORE_INFORMATION }
enum class StageStatus { OK, FORMAT_ERROR, MODEL_ERROR, VALIDATION_ERROR, SKIPPED }

data class NormalizedIncident(val networkAvailable: Boolean?, val httpStatus: Int?, val timeoutObserved: Boolean, val emptyResponse: Boolean, val localHistoryProblem: Boolean, val multipleSignals: Boolean, val normalizedSummary: String)
data class IncidentDecision(val category: IncidentCategory, val severity: IncidentSeverity, val action: IncidentAction, val confidence: Double)
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
