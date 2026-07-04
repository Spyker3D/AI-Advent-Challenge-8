package com.aiassistant.core.domain.entity

data class TokenMetrics(
    val currentRequestTokens: Int,
    val historyTokens: Int,
    val completionTokens: Int?
)