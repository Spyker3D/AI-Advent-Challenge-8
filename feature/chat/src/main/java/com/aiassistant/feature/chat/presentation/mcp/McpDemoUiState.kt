package com.aiassistant.feature.chat.presentation.mcp

data class McpDemoUiState(
    val taskId: String = "AI-17",
    val isLoading: Boolean = false,
    val result: String = "Нажмите кнопку, чтобы вызвать MCP tool",
    val toolsList: String = "",
    val weatherResult: String = "Нажмите кнопку, чтобы вызвать weather MCP tool",
    val isAutoRefreshEnabled: Boolean = false,
    val lastAutoRefreshAt: String = "",
    val autoRefreshIntervalSec: Int = 10
)
