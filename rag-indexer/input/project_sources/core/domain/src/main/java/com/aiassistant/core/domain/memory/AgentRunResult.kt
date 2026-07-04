package com.aiassistant.core.domain.memory

sealed class AgentRunResult {
    data class Success(val content: String) : AgentRunResult()

    data class BlockedByInvariants(
        val message: String,
        val violations: List<String>
    ) : AgentRunResult()
}
