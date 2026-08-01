package com.aiassistant.core.domain.entity

enum class ContextStrategy {
    NONE,
    FULL_HISTORY,
    SLIDING_WINDOW,
    STICKY_FACTS,
    BRANCHING;

    companion object {
        fun fromStoredValue(value: String?): ContextStrategy = when (value) {
            "NO_STRATEGY" -> FULL_HISTORY
            else -> entries.firstOrNull { it.name == value } ?: SLIDING_WINDOW
        }
    }
}
