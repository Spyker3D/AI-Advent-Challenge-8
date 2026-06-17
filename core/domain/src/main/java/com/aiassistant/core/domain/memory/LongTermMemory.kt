package com.aiassistant.core.domain.memory

data class LongTermMemory(
    val profile: String = "",
    val preferences: String = "",
    val globalRules: String = "",
    val projectKnowledge: String = "",
    val decisions: String = ""
)
