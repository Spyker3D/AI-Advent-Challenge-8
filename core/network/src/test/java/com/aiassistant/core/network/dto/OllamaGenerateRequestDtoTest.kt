package com.aiassistant.core.network.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OllamaGenerateRequestDtoTest {

    @Test
    fun serializesQwenRequestWithConservativeOptions() {
        val request = OllamaGenerateRequestDto(
            model = "qwen2.5:7b-instruct",
            prompt = "Hello",
            stream = false,
            options = OllamaOptionsDto(
                temperature = 0.2,
                numPredict = 700,
                numCtx = 8192,
                topP = 0.9,
                repeatPenalty = 1.1,
                seed = 42
            )
        )

        val json = Gson().toJsonTree(request).asJsonObject
        val options = json.getAsJsonObject("options")

        assertEquals("qwen2.5:7b-instruct", json["model"].asString)
        assertFalse(json["stream"].asBoolean)
        assertEquals(0.2, options["temperature"].asDouble, 0.0)
        assertEquals(8192, options["num_ctx"].asInt)
        assertEquals(700, options["num_predict"].asInt)
        assertEquals(0.9, options["top_p"].asDouble, 0.0)
        assertEquals(1.1, options["repeat_penalty"].asDouble, 0.0)
        assertEquals(42, options["seed"].asInt)
    }

    @Test
    fun serializesExactQ5OllamaTag() {
        val json = Gson().toJsonTree(OllamaGenerateRequestDto(
            model = "qwen2.5:7b-instruct-q5_K_M",
            prompt = "same prompt"
        )).asJsonObject

        assertEquals("qwen2.5:7b-instruct-q5_K_M", json["model"].asString)
    }
}
