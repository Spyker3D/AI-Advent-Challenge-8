package com.aiassistant.core.domain.routing

import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import kotlinx.coroutines.CancellationException
import java.util.UUID
import javax.inject.Inject

class ModelRouter @Inject constructor(private val llmClient: LlmClient) {
    suspend fun route(userText: String): Result<RoutingResult> {
        val totalStart = System.nanoTime()
        val input = userText.trim()
        if (input.isEmpty()) return Result.failure(IllegalArgumentException("User request must not be blank"))
        RequestComplexityDetector.detect(input)?.let {
            return callLarge(input, it, null, null, null, totalStart)
        }
        val smallStart = System.nanoTime()
        val small = callModel(input, RoutingConfig.SMALL_MODEL_SYSTEM_PROMPT, RoutingConfig.SMALL_MODEL, RoutingConfig.SMALL_MODEL_MAX_TOKENS)
        val smallLatency = elapsedMs(smallStart)
        val smallResponse = small.getOrElse { error ->
            if (error is CancellationException) throw error
            return callLarge(input, RoutingReason.SMALL_MODEL_ERROR, null, smallLatency, null, totalStart)
        }
        val raw = smallResponse.message
        val decision = SmallModelDecisionParser.parse(raw).getOrElse {
            return callLarge(input, RoutingReason.INVALID_JSON, null, smallLatency, raw, totalStart)
        }
        val reason = when {
            decision.needsEscalation -> RoutingReason.MODEL_REQUESTED_ESCALATION
            decision.confidence < RoutingConfig.CONFIDENCE_THRESHOLD -> RoutingReason.LOW_CONFIDENCE
            decision.answer.length < RoutingConfig.MIN_ANSWER_LENGTH -> RoutingReason.ANSWER_TOO_SHORT
            else -> null
        }
        if (reason != null) return callLarge(input, reason, decision.confidence, smallLatency, raw, totalStart)
        return Result.success(RoutingResult(decision.answer, RoutingConfig.SMALL_MODEL, false, decision.confidence, null, smallLatency, null, elapsedMs(totalStart), raw))
    }

    private suspend fun callLarge(input: String, reason: RoutingReason, confidence: Double?, smallLatency: Long?, raw: String?, totalStart: Long): Result<RoutingResult> {
        val start = System.nanoTime()
        val response = callModel(input, RoutingConfig.LARGE_MODEL_SYSTEM_PROMPT, RoutingConfig.LARGE_MODEL, RoutingConfig.LARGE_MODEL_MAX_TOKENS).getOrElse { error ->
            if (error is CancellationException) throw error
            return Result.failure(ModelRoutingException(RoutingReason.LARGE_MODEL_ERROR, error))
        }
        return Result.success(RoutingResult(response.message, RoutingConfig.LARGE_MODEL, true, confidence, reason, smallLatency, elapsedMs(start), elapsedMs(totalStart), raw))
    }

    private suspend fun callModel(input: String, system: String, model: String, maxTokens: Int): Result<ChatResponse> =
        llmClient.sendChat(listOf(
            Message(UUID.randomUUID().toString(), system, MessageRole.SYSTEM),
            Message(UUID.randomUUID().toString(), input, MessageRole.USER)
        ), maxTokens, model)

    private fun elapsedMs(start: Long) = (System.nanoTime() - start) / 1_000_000L
}

class ModelRoutingException(val reason: RoutingReason, cause: Throwable) : Exception("Large model request failed", cause)
