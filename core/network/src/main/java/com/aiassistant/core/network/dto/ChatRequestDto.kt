package com.aiassistant.core.network.dto

import com.google.gson.annotations.SerializedName

data class ChatRequestDto(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<MessageDto>,
    @SerializedName("temperature")
    val temperature: Float,
    @SerializedName("max_tokens")
    val maxTokens: Int
)