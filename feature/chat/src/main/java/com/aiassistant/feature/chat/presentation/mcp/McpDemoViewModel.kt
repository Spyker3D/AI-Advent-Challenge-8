package com.aiassistant.feature.chat.presentation.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.core.domain.mcp.McpAgentRepository
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
    private val mcpAgentRepository: McpAgentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(McpDemoUiState())
    val uiState = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    fun onTaskIdChanged(value: String) {
        _uiState.update { it.copy(taskId = value) }
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
