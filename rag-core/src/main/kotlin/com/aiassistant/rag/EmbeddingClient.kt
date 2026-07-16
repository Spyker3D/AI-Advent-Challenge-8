package com.aiassistant.rag

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

interface EmbeddingClient {
    suspend fun embed(texts: List<String>): List<List<Float>>
}

data class OllamaEmbeddingConfig(
    val baseUrl: String = "http://localhost:11434",
    val model: String = "nomic-embed-text:latest"
)

class OllamaEmbeddingClient(private val config: OllamaEmbeddingConfig = OllamaEmbeddingConfig()) : EmbeddingClient {
    private val endpoint = "${config.baseUrl.trimEnd('/')}/api/embed"
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()
    private val gson = Gson()

    override suspend fun embed(texts: List<String>): List<List<Float>> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) return@withContext emptyList()
        try {
            val body = gson.toJson(mapOf("model" to config.model, "input" to texts))
            val request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 404 || response.body().contains("not found", ignoreCase = true)) {
                throw IllegalStateException("Embedding model ${config.model} is not installed.\n\nInstall it with:\nollama pull ${config.model}")
            }
            check(response.statusCode() in 200..299) { "Ollama returned HTTP ${response.statusCode()}: ${response.body()}" }
            val embeddings = JsonParser.parseString(response.body()).asJsonObject.getAsJsonArray("embeddings")
                .map { vector -> vector.asJsonArray.map { it.asFloat } }
            check(embeddings.size == texts.size) { "Ollama returned ${embeddings.size} embeddings for ${texts.size} inputs." }
            check(embeddings.all { it.isNotEmpty() && it.size == embeddings.first().size }) { "Ollama returned invalid embedding dimensions." }
            embeddings
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: Exception) {
            val cause = generateSequence(error as Throwable?) { it.cause }.any { it is ConnectException }
            if (cause) throw IllegalStateException("Cannot connect to Ollama at ${config.baseUrl}.\nMake sure Ollama is running.", error)
            throw IllegalStateException("Ollama embedding request failed: ${error.message}", error)
        }
    }
}
