package com.aiassistant.core.data.routing

import android.util.Log
import com.aiassistant.core.data.BuildConfig
import com.aiassistant.core.domain.routing.RoutingDebugMetadata
import com.aiassistant.core.domain.routing.RoutingDiagnosticsLogger
import javax.inject.Inject

class AndroidRoutingDiagnosticsLogger @Inject constructor() : RoutingDiagnosticsLogger {
    override fun log(metadata: RoutingDebugMetadata) {
        if (!BuildConfig.DEBUG) return
        Log.d("ModelRouter", "routingEnabled=${metadata.routingEnabled} firstModel=${metadata.firstModel ?: "not called"} " +
            "finalModel=${metadata.finalModel} escalated=${metadata.escalated} " +
            "confidence=${metadata.confidence ?: "not evaluated"} reason=${metadata.reason ?: "NONE"} " +
            "smallLatencyMs=${metadata.smallLatencyMs ?: "not called"} largeLatencyMs=${metadata.largeLatencyMs ?: "not called"} " +
            "totalLatencyMs=${metadata.totalLatencyMs} contextStrategy=${metadata.contextStrategy} parseFailure=${metadata.parseFailure} structuredFormat=${metadata.structuredFormatEnabled}")
    }
}
