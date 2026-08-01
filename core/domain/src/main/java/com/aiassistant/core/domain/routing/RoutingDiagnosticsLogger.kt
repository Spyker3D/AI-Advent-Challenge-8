package com.aiassistant.core.domain.routing

interface RoutingDiagnosticsLogger {
    fun log(metadata: RoutingDebugMetadata)
}

object NoOpRoutingDiagnosticsLogger : RoutingDiagnosticsLogger {
    override fun log(metadata: RoutingDebugMetadata) = Unit
}
