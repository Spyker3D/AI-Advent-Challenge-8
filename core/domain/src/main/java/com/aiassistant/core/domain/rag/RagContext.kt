package com.aiassistant.core.domain.rag

data class RagContext(
    val prompt: String?,
    val confidence: Float,
    val results: List<RagSearchResult>
)
