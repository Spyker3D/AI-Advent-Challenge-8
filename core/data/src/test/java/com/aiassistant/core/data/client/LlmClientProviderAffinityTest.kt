package com.aiassistant.core.data.client

import com.aiassistant.core.domain.entity.AiProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmClientProviderAffinityTest {
    @Test
    fun `no affinity keeps existing provider selection unchanged`() {
        AiProvider.values().forEach { provider ->
            assertNull(providerAffinityFailure(provider, null))
        }
    }

    @Test
    fun `matching local affinity permits local dispatch`() {
        assertNull(providerAffinityFailure(AiProvider.LOCAL_OLLAMA, AiProvider.LOCAL_OLLAMA))
    }

    @Test
    fun `local affinity rejects external providers before dispatch`() {
        listOf(AiProvider.OPENAI, AiProvider.PRIVATE_VPS).forEach { provider ->
            val failure = providerAffinityFailure(provider, AiProvider.LOCAL_OLLAMA)
            assertTrue(failure is IllegalStateException)
            assertEquals("Required provider LOCAL_OLLAMA is not active", failure?.message)
        }
    }
}
