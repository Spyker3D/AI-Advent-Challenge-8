package com.aiassistant.feature.chat.presentation.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.core.domain.mcp.McpAgentRepository
import com.aiassistant.core.domain.mcp.McpOrchestratorAgent
import com.aiassistant.core.domain.mcp.McpPipelineAgent
import com.aiassistant.core.domain.mcp.McpServerRegistry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val AUTO_REFRESH_INTERVAL_MS = 10_000L

class McpDemoViewModel @Inject constructor(
    private val mcpAgentRepository: McpAgentRepository,
    private val mcpPipelineAgent: McpPipelineAgent,
    private val mcpOrchestratorAgent: McpOrchestratorAgent
) : ViewModel() {
    private val _uiState = MutableStateFlow(McpDemoUiState())
    val uiState = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    fun onTaskIdChanged(value: String) {
        _uiState.update { it.copy(taskId = value) }
    }

    fun onPipelineRequestChanged(value: String) {
        _uiState.update { it.copy(pipelineRequest = value) }
    }

    fun onOrchestrationRequestChanged(value: String) {
        _uiState.update { it.copy(orchestrationRequest = value) }
    }

    fun loadTools() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = mcpAgentRepository.listTools()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    toolsList = result
                )
            }
        }
    }

    fun callTool() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = mcpAgentRepository.checkTaskStatus(_uiState.value.taskId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    result = result
                )
            }
        }
    }

    fun loadWeatherSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = mcpAgentRepository.getWeatherSummary(limit = 10)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    weatherResult = result
                )
            }
        }
    }

    fun loadWeatherHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = mcpAgentRepository.getWeatherHistory(limit = 10)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    weatherResult = result
                )
            }
        }
    }

    fun collectWeatherNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = mcpAgentRepository.collectWeatherNow()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    weatherResult = result
                )
            }
        }
    }

    fun runPipeline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val result = mcpPipelineAgent.run(_uiState.value.pipelineRequest)
                mcpPipelineAgent.formatDebugResult(result)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pipelineResult = result
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pipelineResult = "Ошибка MCP pipeline:\n${throwable.message}"
                    )
                }
            }
        }
    }

    fun runOrchestration() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val tools = mcpOrchestratorAgent.loadToolsFromAllServers()
                val serversText = buildString {
                    appendLine("Registered MCP servers:")
                    McpServerRegistry.mcpServers.forEach { server ->
                        appendLine("- ${server.id}: ${server.name} (${server.endpoint})")
                    }
                    appendLine()
                    appendLine("Tools:")
                    tools.forEach { tool ->
                        appendLine("- ${tool.serverId}.${tool.toolName}: ${tool.description}")
                    }
                }
                val result = mcpOrchestratorAgent.run(_uiState.value.orchestrationRequest)
                serversText to mcpOrchestratorAgent.formatDebugResult(result)
            }.onSuccess { (serversText, result) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        orchestrationServers = serversText,
                        orchestrationResult = result
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        orchestrationResult = "Ошибка MCP orchestration:\n${throwable.message}"
                    )
                }
            }
        }
    }

    fun startAutoRefresh() {
        if (autoRefreshJob?.isActive == true) return

        _uiState.update {
            it.copy(
                isAutoRefreshEnabled = true,
                autoRefreshIntervalSec = (AUTO_REFRESH_INTERVAL_MS / 1_000L).toInt()
            )
        }

        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(isLoading = true) }

                try {
                    val summary = mcpAgentRepository.getWeatherSummary(limit = 10)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            weatherResult = summary,
                            lastAutoRefreshAt = getCurrentTimeText()
                        )
                    }
                } catch (exception: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            weatherResult = "Ошибка авто-сводки:\n${exception.message}",
                            lastAutoRefreshAt = getCurrentTimeText()
                        )
                    }
                }

                delay(AUTO_REFRESH_INTERVAL_MS)
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
        _uiState.update {
            it.copy(
                isAutoRefreshEnabled = false,
                isLoading = false
            )
        }
    }

    fun toggleAutoRefresh() {
        if (_uiState.value.isAutoRefreshEnabled) {
            stopAutoRefresh()
        } else {
            startAutoRefresh()
        }
    }

    override fun onCleared() {
        autoRefreshJob?.cancel()
        super.onCleared()
    }

    private fun getCurrentTimeText(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
