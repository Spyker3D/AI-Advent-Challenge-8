package com.aiassistant.core.network.dto

import com.google.gson.annotations.SerializedName

data class MessageDto(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)