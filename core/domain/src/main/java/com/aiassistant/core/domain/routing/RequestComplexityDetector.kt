package com.aiassistant.core.domain.routing

object RequestComplexityDetector {
    fun detect(text: String): RoutingReason? {
        if (text.length > RoutingConfig.LONG_REQUEST_THRESHOLD) return RoutingReason.LONG_REQUEST
        return RoutingReason.COMPLEX_REQUEST.takeIf {
            RoutingConfig.COMPLEXITY_MARKERS.any { marker -> text.contains(marker, ignoreCase = true) }
        }
    }
}
