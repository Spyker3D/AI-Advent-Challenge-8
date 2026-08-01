package com.aiassistant.core.domain.routing

import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.agent.LlmRequestOptions
import com.aiassistant.core.domain.entity.ContextStrategy
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import kotlinx.coroutines.CancellationException
import java.util.UUID
import javax.inject.Inject

class ModelRouter @Inject constructor(
    private val llmClient: LlmClient,
    private val diagnosticsLogger: RoutingDiagnosticsLogger = NoOpRoutingDiagnosticsLogger
) {
    suspend fun route(userText: String, contextStrategy: ContextStrategy? = null): Result<RoutingResult> = route(
        listOf(Message(UUID.randomUUID().toString(), userText, MessageRole.USER)), contextStrategy
    )

    suspend fun route(preparedMessages: List<Message>, contextStrategy: ContextStrategy? = null): Result<RoutingResult> {
        val totalStart = System.nanoTime()
        val input = preparedMessages.lastOrNull { it.role == MessageRole.USER }?.content?.trim().orEmpty()
        if (input.isEmpty()) return Result.failure(IllegalArgumentException("User request must not be blank"))
        RequestComplexityDetector.detect(input)?.let {
            return callLarge(preparedMessages, it, null, null, totalStart, contextStrategy)
        }
        val smallStart = System.nanoTime()
        val small = callModel(preparedMessages, RoutingPrompts.SMALL_MODEL, RoutingConfig.SMALL_MODEL,
            RoutingConfig.SMALL_MODEL_MAX_TOKENS, smallRoutingOptions)
        val smallLatency = elapsedMs(smallStart)
        val smallResponse = small.getOrElse { error ->
            if (error is CancellationException) throw error
            return callLarge(preparedMessages, RoutingReason.SMALL_MODEL_ERROR, null, smallLatency, totalStart, contextStrategy)
        }
        val parsed = SmallModelDecisionParser.parse(smallResponse.message)
        val decision = parsed.getOrElse { error ->
            return callLarge(preparedMessages, RoutingReason.INVALID_JSON, null, smallLatency, totalStart,
                contextStrategy, SmallModelDecisionParser.failureCategory(error))
        }
        val reason = when {
            decision.needsEscalation -> RoutingReason.MODEL_REQUESTED_ESCALATION
            decision.confidence < RoutingConfig.CONFIDENCE_THRESHOLD -> RoutingReason.LOW_CONFIDENCE
            decision.ambiguity != Ambiguity.LOW -> RoutingReason.MODEL_REQUESTED_ESCALATION
            !decision.sufficientContext -> RoutingReason.MODEL_REQUESTED_ESCALATION
            else -> null
        }
        if (reason != null) return callLarge(preparedMessages, reason, decision.confidence, smallLatency, totalStart, contextStrategy)
        return success(RoutingResult(decision.answer, RoutingConfig.SMALL_MODEL, false, decision.confidence,
            null, smallLatency, null, elapsedMs(totalStart), smallResponse.completionTokens), contextStrategy)
    }

    private suspend fun callLarge(
        messages: List<Message>, reason: RoutingReason, confidence: Double?, smallLatency: Long?,
        totalStart: Long, contextStrategy: ContextStrategy?, parseFailure: RoutingParseFailure? = null
    ): Result<RoutingResult> {
        val start = System.nanoTime()
        val response = callModel(messages, RoutingPrompts.LARGE_MODEL, RoutingConfig.LARGE_MODEL,
            RoutingConfig.LARGE_MODEL_MAX_TOKENS, null).getOrElse { error ->
            if (error is CancellationException) throw error
            return Result.failure(ModelRoutingException(RoutingReason.LARGE_MODEL_ERROR, error))
        }
        return success(RoutingResult(response.message, RoutingConfig.LARGE_MODEL, true, confidence, reason,
            smallLatency, elapsedMs(start), elapsedMs(totalStart), response.completionTokens, parseFailure), contextStrategy)
    }

    private suspend fun callModel(
        messages: List<Message>, tierPrompt: String, model: String, maxTokens: Int,
        options: LlmRequestOptions?
    ): Result<ChatResponse> {
        val applicationSystem = messages.filter { it.role == MessageRole.SYSTEM }.joinToString("\n\n") { it.content }.trim()
        val system = listOf(tierPrompt, applicationSystem).filter { it.isNotBlank() }.joinToString("\n\n")
        val requestMessages = listOf(Message(UUID.randomUUID().toString(), system, MessageRole.SYSTEM)) +
            messages.filterNot { it.role == MessageRole.SYSTEM }
        return if (options == null) llmClient.sendChat(requestMessages, maxTokens, model)
        else llmClient.sendChat(requestMessages, maxTokens, model, options)
    }

    private fun success(result: RoutingResult, contextStrategy: ContextStrategy?): Result<RoutingResult> {
        diagnosticsLogger.log(result.toDebugMetadata(contextStrategy))
        return Result.success(result)
    }

    private fun RoutingResult.toDebugMetadata(strategy: ContextStrategy?) = RoutingDebugMetadata(
        true, RoutingConfig.SMALL_MODEL.takeIf { smallModelLatencyMs != null }, finalModel, escalated,
        smallModelConfidence, escalationReason, smallModelLatencyMs, largeModelLatencyMs, totalLatencyMs,
        strategy, parseFailure, structuredFormatEnabled = smallModelLatencyMs != null
    )

    private fun elapsedMs(start: Long) = (System.nanoTime() - start) / 1_000_000L

    private companion object {
        val smallRoutingOptions = LlmRequestOptions(temperature = 0.0, numPredict = 180, stream = false, jsonSchema = RoutingPrompts.SMALL_MODEL_JSON_SCHEMA)
    }
}

class ModelRoutingException(val reason: RoutingReason, cause: Throwable) : Exception("Large model request failed", cause)
