package com.aiassistant.core.data.rag

import com.aiassistant.core.domain.rag.RagEmbeddingClient
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidOllamaEmbeddingClient @Inject constructor(
    private val gson: Gson
) : RagEmbeddingClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun embed(text: String): Result<List<Float>> = withContext(Dispatchers.IO) {
        runCatching {
            val requestJson = gson.toJson(
                EmbeddingRequest(
                    model = MODEL,
                    prompt = text
                )
            )
            val request = Request.Builder()
                .url(EMBEDDINGS_URL)
                .post(requestJson.toRequestBody(JSON))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Ollama embeddings HTTP ${response.code}: ${response.body?.string().orEmpty()}")
                }
                val body = response.body?.string().orEmpty()
                val embeddingResponse = gson.fromJson(body, EmbeddingResponse::class.java)
                embeddingResponse.embedding
                    ?: error("Ollama response does not contain embedding.")
            }
        }.recoverCatching { throwable ->
            throw IllegalStateException(
                "Ollama is unavailable at $EMBEDDINGS_URL. Start Ollama on the host and run: ollama pull $MODEL",
                throwable
            )
        }
    }

    private data class EmbeddingRequest(
        val model: String,
        val prompt: String
    )

    private data class EmbeddingResponse(
        @SerializedName("embedding") val embedding: List<Float>?
    )

    private companion object {
        const val MODEL = "nomic-embed-text"
        const val EMBEDDINGS_URL = "http://10.0.2.2:11434/api/embeddings"
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
