package com.aiassistant.feature.chat.presentation.routing

import com.aiassistant.core.domain.entity.AiProvider

data class RoutingRequestSnapshot(
    val available: Boolean,
    val enabled: Boolean,
    val modelOverride: String?
)

fun isRoutingToggleVisible(provider: AiProvider): Boolean = provider == AiProvider.LOCAL_OLLAMA

fun routingRequestSnapshot(
    provider: AiProvider,
    routingEnabled: Boolean,
    localModel: String
): RoutingRequestSnapshot {
    val available = provider == AiProvider.LOCAL_OLLAMA
    return RoutingRequestSnapshot(
        available = available,
        enabled = available && routingEnabled,
        modelOverride = localModel.takeIf { available }
    )
}
