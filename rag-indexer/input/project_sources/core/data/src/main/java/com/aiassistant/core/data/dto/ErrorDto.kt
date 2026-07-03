package com.aiassistant.core.data.dto

import com.google.gson.annotations.SerializedName

data class ErrorDto(
    @SerializedName("message")
    val message: String,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("code")
    val code: String? = null
)