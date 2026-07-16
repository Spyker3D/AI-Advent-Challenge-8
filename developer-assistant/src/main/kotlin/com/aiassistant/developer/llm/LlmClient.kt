package com.aiassistant.developer.llm

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

interface LlmClient {
    suspend fun generate(instructions: String, input: String): String
}

data class OpenAiConfig(
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-4.1-mini",
    val apiKey: String
)

class OpenAiResponsesClient(private val config: OpenAiConfig) : LlmClient {
    private val endpoint = "${config.baseUrl.trimEnd('/')}/responses"
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private val gson = Gson()

    override suspend fun generate(instructions: String, input: String): String = withContext(Dispatchers.IO) {
        try {
            val body = gson.toJson(mapOf(
                "model" to config.model,
                "instructions" to instructions,
                "input" to input,
                "store" to false,
                "max_output_tokens" to 1500
            ))
            val request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofMinutes(3))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer ${config.apiKey}")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                val message = runCatching { JsonParser.parseString(response.body()).asJsonObject.getAsJsonObject("error").get("message").asString }.getOrNull()
                throw IllegalStateException("OpenAI Responses API returned HTTP ${response.statusCode()}${message?.let { ": $it" }.orEmpty()}")
            }
            val json = JsonParser.parseString(response.body()).asJsonObject
            val text = json.getAsJsonArray("output").flatMap { item ->
                item.asJsonObject.getAsJsonArray("content")?.mapNotNull { content ->
                    content.asJsonObject.takeIf { it.get("type")?.asString == "output_text" }?.get("text")?.asString
                }.orEmpty()
            }.joinToString("\n").trim()
            check(text.isNotBlank()) { "OpenAI Responses API returned no output text." }
            text
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: Exception) {
            throw IllegalStateException("Cannot connect to OpenAI Responses API at ${config.baseUrl}: ${error.message}", error)
        }
    }
}
