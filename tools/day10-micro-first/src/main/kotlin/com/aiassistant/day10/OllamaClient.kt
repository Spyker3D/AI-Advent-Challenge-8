package com.aiassistant.day10

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

interface OllamaGateway {
    fun embed(texts: List<String>): List<List<Double>>
    fun generate(input: String, correction: Boolean): String
}

class OllamaTransportException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class OllamaClient(
    private val baseUrl: String = "http://127.0.0.1:11434",
    private val microModel: String = MICRO_MODEL,
    private val fallbackModel: String = FALLBACK_MODEL,
    private val timeout: Duration = Duration.ofSeconds(60),
    private val gson: Gson = Gson()
) : OllamaGateway {
    private val http = HttpClient.newBuilder().connectTimeout(timeout).build()

    override fun embed(texts: List<String>): List<List<Double>> {
        val payload = JsonObject().apply {
            addProperty("model", microModel)
            add("input", gson.toJsonTree(texts))
        }
        val response = post("/api/embed", payload)
        val values = response.getAsJsonArray("embeddings") ?: error("Missing embeddings")
        require(values.size() == texts.size) { "Embedding batch size mismatch" }
        return values.map { row -> row.asJsonArray.map { it.asDouble } }
    }

    override fun generate(input: String, correction: Boolean): String {
        val system = if (correction) "$SYSTEM_PROMPT $CORRECTION_PROMPT" else SYSTEM_PROMPT
        val payload = JsonObject().apply {
            addProperty("model", fallbackModel)
            addProperty("system", system)
            addProperty("prompt", input)
            addProperty("stream", false)
            add("format", gson.fromJson(FALLBACK_SCHEMA, JsonObject::class.java))
            add("options", JsonObject().apply { addProperty("temperature", 0) })
        }
        return post("/api/generate", payload).get("response")?.asString ?: error("Missing generated response")
    }

    private fun post(path: String, body: JsonObject): JsonObject {
        try {
            val request = HttpRequest.newBuilder(URI.create(baseUrl.trimEnd('/') + path))
                .timeout(timeout).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body))).build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) throw OllamaTransportException("$path returned HTTP ${response.statusCode()}")
            return gson.fromJson(response.body(), JsonObject::class.java)
                ?: throw OllamaTransportException("$path returned an invalid response")
        } catch (error: OllamaTransportException) {
            throw error
        } catch (error: Exception) {
            throw OllamaTransportException("$path request failed: ${error.message}", error)
        }
    }

    companion object {
        const val SYSTEM_PROMPT = """Use a cause-first algorithm: identify the stated failure cause, not symptoms or advice. NETWORK_UNAVAILABLE means absent connectivity; OPENAI_RATE_LIMIT means excessive frequency, volume, quota, throttling, or HTTP 429; OPENAI_TIMEOUT means elapsed duration, expired deadline, or no timely response; EMPTY_AI_RESPONSE means completed output without usable content; LOCAL_HISTORY_UNAVAILABLE means local chat history cannot be read; AMBIGUOUS means insufficient or conflicting causes. Retry-later advice alone does not determine a category. Return exactly category, confidence, and a short Russian reason."""
        const val CORRECTION_PROMPT = "The previous output was structurally invalid. Return one valid object only."
        const val FALLBACK_SCHEMA = """{"type":"object","properties":{"category":{"type":"string","enum":["NETWORK_UNAVAILABLE","OPENAI_RATE_LIMIT","OPENAI_TIMEOUT","EMPTY_AI_RESPONSE","LOCAL_HISTORY_UNAVAILABLE","AMBIGUOUS"]},"confidence":{"type":"number","minimum":0,"maximum":1},"reason":{"type":"string","minLength":1,"maxLength":160}},"required":["category","confidence","reason"],"additionalProperties":false}"""
    }
}
