package com.aiassistant.core.data.client

import com.aiassistant.core.domain.agent.LlmRequestOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OllamaRoutingFormatTest {
    @Test fun `routing schema maps to object format`() {
        val format = LlmRequestOptions(jsonSchema = """{"type":"object","required":["ambiguity"]}""").toOllamaFormat()
        assertTrue(format!!.isJsonObject)
        assertEquals("object", format.asJsonObject["type"].asString)
    }

    @Test fun `ordinary request has no format`() {
        assertNull(LlmRequestOptions().toOllamaFormat())
    }
}