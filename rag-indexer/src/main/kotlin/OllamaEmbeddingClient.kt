import java.io.IOException
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class OllamaEmbeddingClient(
    private val endpoint: String = "http://localhost:11434/api/embeddings",
    private val model: String = "nomic-embed-text"
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun embed(text: String): List<Float> {
        var lastError: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody(text)))
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() in 200..299) {
                    return parseEmbedding(response.body())
                }
                lastError = IOException("Ollama returned HTTP ${response.statusCode()}: ${response.body()}")
            } catch (error: Exception) {
                lastError = error
            }

            if (attempt < MAX_ATTEMPTS - 1) {
                Thread.sleep(500L * (attempt + 1))
            }
        }

        val error = lastError
        if (error is ConnectException) {
            throw IllegalStateException(
                "Ollama is not available at $endpoint. Start Ollama and run: ollama pull $model",
                error
            )
        }
        throw IllegalStateException(
            "Failed to generate embedding with Ollama model '$model' after $MAX_ATTEMPTS attempts: ${error?.message}",
            error
        )
    }

    private fun requestBody(text: String): String {
        return """{"model":"${jsonEscape(model)}","prompt":"${jsonEscape(text)}"}"""
    }

    private fun parseEmbedding(json: String): List<Float> {
        val key = """"embedding""""
        val keyIndex = json.indexOf(key)
        require(keyIndex >= 0) { "Ollama response does not contain an embedding field." }

        val arrayStart = json.indexOf('[', startIndex = keyIndex)
        require(arrayStart >= 0) { "Ollama embedding field is not an array." }

        var depth = 0
        var arrayEnd = -1
        for (index in arrayStart until json.length) {
            when (json[index]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        arrayEnd = index
                        break
                    }
                }
            }
        }
        require(arrayEnd > arrayStart) { "Ollama embedding array is malformed." }

        return json.substring(arrayStart + 1, arrayEnd)
            .split(',')
            .mapNotNull { value ->
                value.trim().takeIf { it.isNotEmpty() }?.toFloat()
            }
    }

    private fun jsonEscape(value: String): String {
        return buildString(value.length) {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (char.code < 0x20) {
                            append("\\u")
                            append(char.code.toString(16).padStart(4, '0'))
                        } else {
                            append(char)
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
