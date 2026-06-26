package com.aiassistant.core.domain.mcp

interface McpAgentRepository {
    suspend fun listTools(): String
    suspend fun checkTaskStatus(taskId: String): String
}
