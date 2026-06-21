package com.aiassistant.feature.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.core.domain.memory.TaskContext
import com.aiassistant.core.domain.memory.TaskPipelineOrchestrator
import com.aiassistant.core.domain.invariant.ArchitectureInvariant
import com.aiassistant.core.domain.invariant.BudgetInvariant
import com.aiassistant.core.domain.invariant.MaxDependenciesInvariant
import com.aiassistant.core.domain.invariant.StackInvariant
import com.aiassistant.core.domain.repository.ChatRepository
import com.aiassistant.core.domain.repository.LongTermMemoryRepository
import com.aiassistant.core.domain.repository.InvariantRepository
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
    private val invariantRepository: InvariantRepository,
    private val chatRepository: ChatRepository,
    private val taskPipelineOrchestrator: TaskPipelineOrchestrator
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
                val invariants = invariantRepository.getInvariants()
                val stack = invariants.filterIsInstance<StackInvariant>().firstOrNull()
                _uiState.value = _uiState.value.copy(
                    activeTaskContext = taskContext,
                    longTermMemory = longTermMemory,
                    allowedStack = stack?.allowed?.joinToString("\n").orEmpty(),
                    bannedStack = stack?.banned?.joinToString("\n").orEmpty(),
                    architecture = invariants
                        .filterIsInstance<ArchitectureInvariant>()
                        .firstOrNull()
                        ?.required
                        .orEmpty(),
                    bannedArchitectures = invariants
                        .filterIsInstance<ArchitectureInvariant>()
                        .firstOrNull()
                        ?.banned
                        ?.joinToString("\n")
                        .orEmpty(),
                    budget = invariants
                        .filterIsInstance<BudgetInvariant>()
                        .firstOrNull()
                        ?.rule
                        .orEmpty(),
                    maxDependencies = invariants
                        .filterIsInstance<MaxDependenciesInvariant>()
                        .firstOrNull()
                        ?.max
                        ?.toString()
                        .orEmpty(),
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

    fun updateInvariantForm(
        allowedStack: String,
        bannedStack: String,
        architecture: String,
        bannedArchitectures: String,
        budget: String,
        maxDependencies: String
    ) {
        _uiState.value = _uiState.value.copy(
            allowedStack = allowedStack,
            bannedStack = bannedStack,
            architecture = architecture,
            bannedArchitectures = bannedArchitectures,
            budget = budget,
            maxDependencies = maxDependencies
        )
    }

    fun saveInvariants() {
        val state = _uiState.value
        val allowed = state.allowedStack.toInvariantSet()
        val banned = state.bannedStack.toInvariantSet()
        val bannedArchitectures = state.bannedArchitectures.toInvariantSet()
        val max = state.maxDependencies.toIntOrNull()

        if (allowed.isEmpty() || allowed.any(banned::contains) ||
            state.architecture.isBlank() ||
            state.budget.isBlank() || max == null || max < 0
        ) {
            _uiState.value = state.copy(
                error = "Заполните поля корректно; allowed и banned stack не должны пересекаться"
            )
            return
        }

        viewModelScope.launch {
            runCatching {
                invariantRepository.saveInvariants(
                    listOf(
                        StackInvariant(allowed = allowed, banned = banned),
                        ArchitectureInvariant(
                            required = state.architecture.trim(),
                            banned = bannedArchitectures
                        ),
                        BudgetInvariant(rule = state.budget.trim()),
                        MaxDependenciesInvariant(max = max)
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(message = "Invariants saved", error = null)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    error = throwable.message ?: "Failed to save invariants"
                )
            }
        }
    }

    fun pauseTask() = updateTask { task ->
        taskPipelineOrchestrator.pauseTask(task.id)
    }

    fun startTask() = updateTask { task ->
        taskPipelineOrchestrator.startTask(
            task.relatedChatIds.firstOrNull() ?: "main",
            task.id
        )
    }

    fun resumeTask() = updateTask { task ->
        taskPipelineOrchestrator.resumeTask(
            task.relatedChatIds.firstOrNull() ?: "main",
            task.id
        )
    }

    fun continueTask() = updateTask { task ->
        taskPipelineOrchestrator.confirmNextStage(
            task.relatedChatIds.firstOrNull() ?: "main",
            task.id
        )
    }

    private fun updateTask(action: suspend (TaskContext) -> TaskContext) {
        val task = _uiState.value.activeTaskContext ?: return
        viewModelScope.launch {
            runCatching { action(task) }
                .onSuccess { updated ->
                    updated.relatedChatIds.forEach { chatId ->
                        chatRepository.updateChatActiveTaskContext(chatId, updated.id)
                    }
                    _uiState.value = _uiState.value.copy(activeTaskContext = updated)
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        error = throwable.message ?: "Task action failed"
                    )
                }
        }
    }

    private fun String.toInvariantSet(): Set<String> =
        lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toCollection(linkedSetOf())
}
