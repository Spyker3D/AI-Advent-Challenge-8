package com.aiassistant.feature.chat.presentation.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.core.domain.mcp.McpAgentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class McpDemoViewModel @Inject constructor(
    private val mcpAgentRepository: McpAgentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(McpDemoUiState())
    val uiState = _uiState.asStateFlow()

    fun onTaskIdChanged(value: String) {
        _uiState.value = _uiState.value.copy(taskId = value)
    }

    fun loadTools() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = mcpAgentRepository.listTools()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                toolsList = result
            )
        }
    }

    fun callTool() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = mcpAgentRepository.checkTaskStatus(_uiState.value.taskId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                result = result
            )
        }
    }
}
