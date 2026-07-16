package com.aiassistant.core.network.dto

import com.google.gson.annotations.SerializedName

data class OllamaGenerateRequestDto(
    val model: String,
    val prompt: String,
    val system: String? = null,
    val stream: Boolean = false,
    val options: OllamaOptionsDto? = null
)

data class OllamaOptionsDto(
    val temperature: Double? = null,
    @SerializedName("num_ctx")
    val numCtx: Int? = null,
    @SerializedName("num_predict")
    val numPredict: Int? = null,
    @SerializedName("top_p") val topP: Double? = null,
    @SerializedName("repeat_penalty") val repeatPenalty: Double? = null,
    val seed: Int? = null
)
