package com.aiassistant.core.domain.entity

data class AiResponseMetadata(
    val modelDisplayName: String,
    val modelApiName: String,
    val responseTimeMs: Long,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?,
    val estimatedCostUsd: Double?,
    val localMetrics: LocalGenerationMetrics? = null
)
