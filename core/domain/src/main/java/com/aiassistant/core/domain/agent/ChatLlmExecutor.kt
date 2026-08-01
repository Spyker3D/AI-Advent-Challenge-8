package com.aiassistant.core.domain.agent

import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.ContextStrategy
import com.aiassistant.core.domain.routing.ModelRouter
import com.aiassistant.core.domain.routing.RoutingConfig
import com.aiassistant.core.domain.routing.RoutingDebugMetadata
import com.aiassistant.core.domain.routing.RoutingDiagnosticsLogger
import javax.inject.Inject

data class ChatExecutionResult(
    val response: ChatResponse,
    val routingMetadata: RoutingDebugMetadata?,
    val finalModel: String?
)

class ChatLlmExecutor @Inject constructor(
    private val llmClient: LlmClient,
    private val modelRouter: ModelRouter,
    private val diagnosticsLogger: RoutingDiagnosticsLogger
) {
    suspend fun execute(
        messages: List<Message>,
        maxTokens: Int?,
        model: String?,
        routingAvailable: Boolean,
        routingEnabled: Boolean,
        contextStrategy: ContextStrategy? = null
    ): Result<ChatExecutionResult> {
        if (!routingAvailable || !routingEnabled) {
            return llmClient.sendChat(messages, maxTokens, model).map { response ->
                val metadata = if (routingAvailable) {
                    RoutingDebugMetadata(
                        routingEnabled = false,
                        firstModel = null,
                        finalModel = response.metadata?.modelApiName ?: model.orEmpty(),
                        escalated = false,
                        confidence = null,
                        reason = null,
                        smallLatencyMs = null,
                        largeLatencyMs = null,
                        totalLatencyMs = response.metadata?.responseTimeMs,
                        contextStrategy = contextStrategy
                    ).also(diagnosticsLogger::log)
                } else {
                    null
                }
                ChatExecutionResult(response, metadata, response.metadata?.modelApiName ?: model)
            }
        }
        return modelRouter.route(messages, contextStrategy).map { routed ->
            ChatExecutionResult(
                response = ChatResponse(routed.answer, routed.completionTokens),
                routingMetadata = RoutingDebugMetadata(
                    routingEnabled = true,
                    firstModel = RoutingConfig.SMALL_MODEL.takeIf { routed.smallModelLatencyMs != null },
                    finalModel = routed.finalModel,
                    escalated = routed.escalated,
                    confidence = routed.smallModelConfidence,
                    reason = routed.escalationReason,
                    smallLatencyMs = routed.smallModelLatencyMs,
                    largeLatencyMs = routed.largeModelLatencyMs,
                    totalLatencyMs = routed.totalLatencyMs,
                    contextStrategy = contextStrategy,
                    parseFailure = routed.parseFailure,
                    structuredFormatEnabled = routed.smallModelLatencyMs != null
                ),
                finalModel = routed.finalModel
            )
        }
    }
}
