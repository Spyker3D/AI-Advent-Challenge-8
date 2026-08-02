package com.aiassistant.core.data.rag

import com.aiassistant.core.domain.rag.RagEmbeddingClient
import com.aiassistant.core.domain.microfirst.EmbeddingClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidOllamaEmbeddingClient @Inject constructor(
    private val embeddingClient: EmbeddingClient
) : RagEmbeddingClient {
    override suspend fun embed(text: String): Result<List<Float>> =
        embeddingClient.embed(listOf(text), MODEL).map { it.single() }

    private companion object {
        const val MODEL = "nomic-embed-text"
    }
}
