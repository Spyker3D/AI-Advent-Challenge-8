package com.aiassistant.core.network.dto

import com.google.gson.annotations.SerializedName

data class ChatResponseDto(
    @SerializedName("choices")
    val choices: List<ChoiceDto>,
    @SerializedName("usage")
    val usage: UsageDto? = null,
    @SerializedName("error")
    val error: ErrorDto? = null
)