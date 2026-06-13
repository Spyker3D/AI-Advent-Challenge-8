package com.aiassistant.core.domain.util

object TokenCounter {
    fun countTokens(text: String): Int {
        return if (text.isBlank()) 0 else (text.length / 4).coerceAtLeast(1)
    }
}