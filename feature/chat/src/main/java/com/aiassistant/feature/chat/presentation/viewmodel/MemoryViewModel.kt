package com.aiassistant.feature.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.core.domain.memory.TaskContext
import com.aiassistant.core.domain.repository.ChatRepository
import com.aiassistant.core.domain.repository.LongTermMemoryRepository
import com.aiassistant.core.domain.repository.WorkingMemoryRepository
import com.aiassistant.feature.chat.presentation.memory.MemoryFileType
import com.aiassistant.feature.chat.presentation.memory.MemoryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class MemoryViewModel @Inject constructor(
    private val workingMemoryRepository: WorkingMemoryRepository,
    private val longTermMemoryRepository: LongTermMemoryRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    init {
        loadMemory()
    }

    fun loadMemory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                val taskContext = workingMemoryRepository.getActiveTaskContext()
                val longTermMemory = longTermMemoryRepository.getLongTermMemory()
                _uiState.value = _uiState.value.copy(
                    activeTaskContext = taskContext,
                    longTermMemory = longTermMemory,
                    isLoading = false
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = throwable.message ?: "Failed to load memory"
                )
            }
        }
    }

    fun saveTaskContext(taskContext: TaskContext) {
        viewModelScope.launch {
            runCatching {
                workingMemoryRepository.saveTaskContext(taskContext)
                workingMemoryRepository.setActiveTaskContext(taskContext.id)
                taskContext.relatedChatIds.forEach { chatId ->
                    chatRepository.updateChatActiveTaskContext(chatId, taskContext.id)
                }
                _uiState.value = _uiState.value.copy(
                    activeTaskContext = taskContext.copy(updatedAt = System.currentTimeMillis()),
                    message = "Task context saved"
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    error = throwable.message ?: "Failed to save task context"
                )
            }
        }
    }

    fun saveMarkdownMemory(type: MemoryFileType, content: String) {
        viewModelScope.launch {
            runCatching {
                when (type) {
                    MemoryFileType.PROFILE -> longTermMemoryRepository.saveProfile(content)
                    MemoryFileType.PREFERENCES -> longTermMemoryRepository.savePreferences(content)
                    MemoryFileType.GLOBAL_RULES -> longTermMemoryRepository.saveGlobalRules(content)
                    MemoryFileType.PROJECT_KNOWLEDGE -> longTermMemoryRepository.saveProjectKnowledge(content)
                    MemoryFileType.DECISIONS -> longTermMemoryRepository.saveDecisions(content)
                }
                _uiState.value = _uiState.value.copy(
                    longTermMemory = longTermMemoryRepository.getLongTermMemory(),
                    message = "Memory file saved"
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    error = throwable.message ?: "Failed to save memory file"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}
