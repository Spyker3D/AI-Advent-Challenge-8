package com.aiassistant.day10

const val MICRO_MODEL = "nomic-embed-text:latest"
const val FALLBACK_MODEL = "qwen2.5:7b-instruct"
const val MIN_SCORE = 0.70
const val MIN_MARGIN = 0.06
const val MAX_REASON_LENGTH = 160
val FALLBACK_REASONS = listOf(
    "LOW_SCORE", "LOW_MARGIN", "EMBEDDING_ERROR", "INVALID_VECTOR",
    "PROTOTYPE_INITIALIZATION_ERROR", "MICRO_RESULT_INVALID"
)

val LABELS = listOf(
    "NETWORK_UNAVAILABLE", "OPENAI_RATE_LIMIT", "OPENAI_TIMEOUT",
    "EMPTY_AI_RESPONSE", "LOCAL_HISTORY_UNAVAILABLE"
)
val ALL_LABELS = LABELS + "AMBIGUOUS"

data class TestCase(
    val id: String,
    val group: String,
    val input: String,
    val expectedLabel: String,
    val expectedRoute: String
)

data class FallbackResponse(val category: String, val confidence: Double, val reason: String)

data class MicroDecision(
    val label: String?, val topScore: Double?, val secondScore: Double?, val margin: Double?,
    val status: String, val fallbackReason: String?
)

data class ResultRow(
    val id: String, val group: String, val input: String,
    val expectedLabel: String, val expectedRoute: String,
    val microLabel: String?, val topScore: Double?, val secondScore: Double?, val margin: Double?,
    val microStatus: String, val actualRoute: String, val finalLabel: String?,
    val correctLabel: Boolean, val correctRoute: Boolean, val fallbackReason: String?,
    val microLatencyMs: Double, val fallbackLatencyMs: Double, val totalLatencyMs: Double,
    val largeLlmCalls: Int, val error: String?
)

data class RunSummary(
    val results: List<ResultRow>,
    val totalRequests: Int,
    val microHandled: Int,
    val fallbackHandled: Int,
    val largeModelCalls: Int,
    val averageLatencyMs: Double
)
