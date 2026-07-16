package com.aiassistant.core.network.dto

import com.google.gson.annotations.SerializedName

data class PrivateVpsChatRequestDto(
    val model: String,
    val messages: List<PrivateVpsMessageDto>,
    val temperature: Double? = null,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false
)

data class PrivateVpsMessageDto(val role: String, val content: String)

data class PrivateVpsChatResponseDto(
    val id: String?, val model: String?, val choices: List<PrivateVpsChoiceDto>?,
    val usage: PrivateVpsUsageDto?
) {
    fun outputText(): String = choices?.firstOrNull()?.message?.content?.trim().orEmpty()
}

data class PrivateVpsChoiceDto(
    val index: Int?, val message: PrivateVpsMessageDto?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class PrivateVpsUsageDto(
    @SerializedName("input_tokens") val inputTokens: Int? = null,
    @SerializedName("output_tokens") val outputTokens: Int? = null,
    @SerializedName("total_tokens") val totalTokens: Int? = null,
    @SerializedName("prompt_tokens") val promptTokens: Int? = null,
    @SerializedName("completion_tokens") val completionTokens: Int? = null,
    @SerializedName("response_token/s") val responseTokensPerSecond: Double? = null,
    @SerializedName("total_duration") val totalDurationNs: Long? = null,
    @SerializedName("load_duration") val loadDurationNs: Long? = null
) {
    fun effectiveInputTokens() = inputTokens ?: promptTokens
    fun effectiveOutputTokens() = outputTokens ?: completionTokens
}

data class PrivateVpsModelsResponseDto(val data: List<PrivateVpsModelDto>? = null)
data class PrivateVpsModelDto(val id: String? = null, val name: String? = null)
