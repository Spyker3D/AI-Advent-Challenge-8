package com.aiassistant.core.domain.entity

data class StickyFacts(
    val goal: String = "",
    val stack: String = "",
    val constraints: String = "",
    val preferences: String = "",
    val decisions: String = "",
    val unresolvedQuestions: String = ""
)