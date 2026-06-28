package com.aiassistant.feature.chat.presentation.mcp

data class McpDemoUiState(
    val taskId: String = "AI-17",
    val isLoading: Boolean = false,
    val result: String = "Нажмите кнопку, чтобы вызвать MCP tool",
    val toolsList: String = "",
    val weatherResult: String = "Нажмите кнопку, чтобы вызвать weather MCP tool",
    val isAutoRefreshEnabled: Boolean = false,
    val lastAutoRefreshAt: String = "",
    val autoRefreshIntervalSec: Int = 10,
    val pipelineRequest: String = "Подготовь отчет о погоде в Санкт-Петербурге",
    val pipelineResult: String = "Нажмите кнопку, чтобы запустить MCP pipeline",
    val orchestrationRequest: String = "Подготовь отчет о погоде в Санкт-Петербурге, сохрани его в заметки и создай задачу проверить погоду завтра",
    val orchestrationServers: String = "",
    val orchestrationResult: String = "Нажмите кнопку, чтобы запустить MCP orchestration flow"
)
