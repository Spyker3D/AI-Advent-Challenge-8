package com.aiassistant.core.domain.rag

data class RagSearchResult(
    val chunk: RagChunk,
    val finalScore: Float,
    val cosineScore: Float,
    val keywordScore: Float,
    val metadataScore: Float
)
