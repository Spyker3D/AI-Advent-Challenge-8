package com.aiassistant.feature.chat.presentation

data class RagSourceUi(
    val source: String,
    val section: String?,
    val finalScore: Float,
    val cosineScore: Float,
    val keywordScore: Float,
    val metadataScore: Float
)
