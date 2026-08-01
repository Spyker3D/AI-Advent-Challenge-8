package com.aiassistant.feature.chat.presentation.routing

import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.routing.RoutingDebugMetadata
import com.aiassistant.feature.chat.presentation.ChatUiState
import org.junit.Assert.*
import org.junit.Test

class RoutingUiPolicyTest {
    @Test fun `routing defaults off`() { assertFalse(ChatUiState().routingEnabled) }
    @Test fun `toggle is visible only for local ollama`() {
        assertTrue(isRoutingToggleVisible(AiProvider.LOCAL_OLLAMA))
        assertFalse(isRoutingToggleVisible(AiProvider.OPENAI))
        assertFalse(isRoutingToggleVisible(AiProvider.PRIVATE_VPS))
    }
    @Test fun `snapshot enables only local ollama and preserves local model`() {
        val local = routingRequestSnapshot(AiProvider.LOCAL_OLLAMA, true, "local:model")
        assertTrue(local.available); assertTrue(local.enabled); assertEquals("local:model", local.modelOverride)
        listOf(AiProvider.OPENAI, AiProvider.PRIVATE_VPS).forEach {
            val external = routingRequestSnapshot(it, true, "local:model")
            assertFalse(external.available); assertFalse(external.enabled); assertNull(external.modelOverride)
        }
    }
    @Test fun `metadata remains associated with its assistant message id`() {
        val one = RoutingDebugMetadata(true, "small", "small", false, .9, null, 1, null, 2)
        val two = RoutingDebugMetadata(true, "small", "large", true, .5, null, 1, 2, 3)
        val state = ChatUiState(routingDebugByMessageId = mapOf("a1" to one, "a2" to two))
        assertEquals("small", state.routingDebugByMessageId["a1"]!!.finalModel)
        assertEquals("large", state.routingDebugByMessageId["a2"]!!.finalModel)
    }
}
