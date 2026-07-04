package com.aiassistant.core.data.dto

import com.google.gson.annotations.SerializedName

data class ChoiceDto(
    @SerializedName("message")
    val message: MessageDto,
    @SerializedName("finish_reason")
    val finishReason: String? = null,
    @SerializedName("index")
    val index: Int = 0
)