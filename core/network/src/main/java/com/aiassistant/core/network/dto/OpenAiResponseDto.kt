package com.aiassistant.core.network.dto

import com.google.gson.annotations.SerializedName

data class OpenAiResponseRequestDto(
    val model: String,
    val input: List<OpenAiInputMessageDto>,
    val temperature: Double? = null,
    @SerializedName("max_output_tokens") val maxOutputTokens: Int? = null
)

data class OpenAiInputMessageDto(val role: String, val content: String)

data class OpenAiResponseDto(
    val id: String?, val model: String?, val output: List<OpenAiOutputItemDto>?, val usage: OpenAiUsageDto?
) {
    fun outputText(): String = output.orEmpty().flatMap { it.content.orEmpty() }
        .filter { it.type == "output_text" }.mapNotNull { it.text }.joinToString("").trim()
}

data class OpenAiOutputItemDto(val type: String?, val role: String?, val content: List<OpenAiOutputContentDto>?)
data class OpenAiOutputContentDto(val type: String?, val text: String?)
data class OpenAiUsageDto(
    @SerializedName("input_tokens") val inputTokens: Int?,
    @SerializedName("output_tokens") val outputTokens: Int?,
    @SerializedName("total_tokens") val totalTokens: Int?
)
