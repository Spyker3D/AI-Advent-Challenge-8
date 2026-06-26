package com.aiassistant.core.data.mcp

import com.aiassistant.core.domain.mcp.McpAgentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpAgentRepositoryImpl @Inject constructor(
    private val mcpClient: McpClient
) : McpAgentRepository {
    override suspend fun listTools(): String = withContext(Dispatchers.IO) {
        mcpClient.listTools()
    }

    override suspend fun checkTaskStatus(taskId: String): String = withContext(Dispatchers.IO) {
        mcpClient.callGetTaskStatus(taskId)
    }

    override suspend fun getWeatherSummary(limit: Int): String = withContext(Dispatchers.IO) {
        mcpClient.callGetWeatherSummary(limit)
    }

    override suspend fun getWeatherHistory(limit: Int): String = withContext(Dispatchers.IO) {
        mcpClient.callGetWeatherHistory(limit)
    }

    override suspend fun collectWeatherNow(): String = withContext(Dispatchers.IO) {
        mcpClient.callCollectWeatherNow()
    }
}
