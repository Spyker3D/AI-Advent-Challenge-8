package com.aiassistant.core.data.microfirst

import com.aiassistant.core.domain.microfirst.EmbeddingClient
import com.aiassistant.core.domain.repository.SettingsRepository
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.network.api.OllamaApi
import com.aiassistant.core.network.api.OllamaApiFactory
import com.aiassistant.core.network.dto.OllamaEmbedRequestDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OllamaEmbeddingClient private constructor(
    private val settingsRepository: SettingsRepository,
    private val createApi: (String) -> OllamaApi
) : EmbeddingClient {

    @Inject
    constructor(
        settingsRepository: SettingsRepository,
        apiFactory: OllamaApiFactory
    ) : this(settingsRepository, apiFactory::create)

    internal constructor(
        settingsRepository: SettingsRepository,
        api: OllamaApi
    ) : this(settingsRepository, { api })

    override suspend fun embed(texts: List<String>, model: String): Result<List<List<Float>>> {
        if (texts.isEmpty()) return Result.success(emptyList())
        if (model.isBlank()) return Result.failure(IllegalArgumentException("Embedding model must not be blank."))

        return try {
            val baseUrl = settingsRepository.getChatSettings().first().localBaseUrl
                .ifBlank { ChatSettings.DEFAULT_LOCAL_BASE_URL }
            val response = createApi(baseUrl).embed(
                OllamaEmbedRequestDto(model = model.trim(), input = texts)
            )
            Result.success(validateEmbeddings(response.embeddings, texts.size))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (http: HttpException) {
            Result.failure(IllegalStateException("Ollama embed request failed with HTTP ${http.code()}.", http))
        } catch (throwable: Throwable) {
            Result.failure(IllegalStateException("Ollama embed request failed.", throwable))
        }
    }

    private fun validateEmbeddings(
        embeddings: List<List<Float>>?,
        expectedCount: Int
    ): List<List<Float>> {
        require(!embeddings.isNullOrEmpty()) { "Ollama returned no embeddings." }
        require(embeddings.size == expectedCount) { "Ollama returned an unexpected embedding count." }
        val dimension = embeddings.first().size
        require(dimension > 0) { "Ollama returned an empty embedding." }
        require(embeddings.all { it.size == dimension }) { "Ollama returned inconsistent embedding dimensions." }
        require(embeddings.flatten().all(Float::isFinite)) { "Ollama returned a non-finite embedding value." }
        return embeddings
    }
}
