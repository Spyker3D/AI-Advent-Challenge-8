package com.aiassistant.core.data.config

data class ApiConfig(
    val openAiApiKey: String,
    val privateVpsBaseUrl: String = "",
    val privateVpsApiKey: String = "",
    val privateVpsModel: String = "qwen2.5:3b"
)
