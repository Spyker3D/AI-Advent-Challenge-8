package com.aiassistant.core.domain.rag

data class RagAnswerConfig(
    val minimumConfidence: Float = 0.45f,
    val minimumSpecificTerms: Int = 2
)
