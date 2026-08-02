package com.aiassistant.core.network.dto

data class OllamaEmbedRequestDto(
    val model: String,
    val input: List<String>
)
