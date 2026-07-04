package com.aiassistant.core.network.dto

import com.google.gson.annotations.SerializedName

data class ProviderDto(
    @SerializedName("order")
    val order: List<String>? = null,

    @SerializedName("allow_fallbacks")
    val allowFallbacks: Boolean = false
)