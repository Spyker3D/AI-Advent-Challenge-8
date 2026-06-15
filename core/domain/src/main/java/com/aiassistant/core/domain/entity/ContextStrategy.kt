package com.aiassistant.core.domain.entity

enum class ContextStrategy {
    NO_STRATEGY,
    SLIDING_WINDOW,
    STICKY_FACTS,
    BRANCHING
}