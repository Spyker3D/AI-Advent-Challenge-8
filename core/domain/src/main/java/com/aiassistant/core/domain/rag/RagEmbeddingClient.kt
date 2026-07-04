package com.aiassistant.core.domain.rag

interface RagEmbeddingClient {
    suspend fun embed(text: String): Result<List<Float>>
}
