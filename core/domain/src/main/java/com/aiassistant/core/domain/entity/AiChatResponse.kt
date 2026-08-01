package com.aiassistant.core.domain.entity

import com.aiassistant.core.domain.routing.RoutingDebugMetadata

data class AiChatResponse(
    val message: String,
    val metadata: AiResponseMetadata?,
    val tokenMetrics: TokenMetrics? = null,
    val routingDebugMetadata: RoutingDebugMetadata? = null
)
