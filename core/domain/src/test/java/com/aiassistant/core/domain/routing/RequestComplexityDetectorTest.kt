package com.aiassistant.core.domain.routing

import org.junit.Assert.*
import org.junit.Test

class RequestComplexityDetectorTest {
    @Test fun `long request is detected first`() { assertEquals(RoutingReason.LONG_REQUEST, RequestComplexityDetector.detect("x".repeat(401))) }
    @Test fun `explicit marker is case insensitive`() { assertEquals(RoutingReason.COMPLEX_REQUEST, RequestComplexityDetector.detect("ПРОАНАЛИЗИРУЙ АРХИТЕКТУРУ проекта")) }
    @Test fun `single compare word is not broad marker`() { assertNull(RequestComplexityDetector.detect("Сравни val и var")) }
}
