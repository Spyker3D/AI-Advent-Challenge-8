package com.aiassistant.core.network.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class OllamaEmbedDtoTest {
    private val gson = Gson()

    @Test
    fun `serializes batch input for official embed endpoint`() {
        val json = gson.toJsonTree(
            OllamaEmbedRequestDto("nomic-embed-text", listOf("first", "second"))
        ).asJsonObject

        assertEquals("nomic-embed-text", json["model"].asString)
        assertEquals(listOf("first", "second"), json["input"].asJsonArray.map { it.asString })
    }

    @Test
    fun `deserializes batch embeddings`() {
        val response = gson.fromJson(
            """{"embeddings":[[1.0,2.0],[3.0,4.0]]}""",
            OllamaEmbedResponseDto::class.java
        )

        assertEquals(listOf(listOf(1f, 2f), listOf(3f, 4f)), response.embeddings)
    }
}
