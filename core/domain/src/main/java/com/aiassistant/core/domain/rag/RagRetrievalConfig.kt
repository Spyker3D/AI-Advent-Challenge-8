package com.aiassistant.core.domain.rag

data class RagRetrievalConfig(
    val candidateTopK: Int = 20,
    val finalTopK: Int = 5,
    val similarityThreshold: Float = 0.55f,
    val cosineWeight: Float = 0.70f,
    val keywordWeight: Float = 0.20f,
    val metadataWeight: Float = 0.10f,
    val queryRewriteEnabled: Boolean = true,
    val rerankingEnabled: Boolean = true,
    val filteringEnabled: Boolean = true
) {
    companion object {
        val Baseline = RagRetrievalConfig(
            candidateTopK = 5,
            finalTopK = 5,
            queryRewriteEnabled = false,
            rerankingEnabled = false,
            filteringEnabled = false
        )

        val Improved = RagRetrievalConfig()
    }
}
