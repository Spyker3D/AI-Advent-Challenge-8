package com.aiassistant.core.domain.mcp

interface McpAgentRepository {
    suspend fun listTools(): String
    suspend fun callTool(name: String, arguments: Map<String, Any?>): String
    suspend fun checkTaskStatus(taskId: String): String
    suspend fun getWeatherSummary(limit: Int): String
    suspend fun getWeatherHistory(limit: Int): String
    suspend fun collectWeatherNow(): String
}
