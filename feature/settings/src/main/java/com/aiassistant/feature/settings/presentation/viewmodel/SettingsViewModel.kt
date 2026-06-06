package com.aiassistant.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.usecase.GetChatSettingsUseCase
import com.aiassistant.core.domain.usecase.SaveChatSettingsUseCase
import com.aiassistant.feature.settings.presentation.SettingsUiEvent
import com.aiassistant.feature.settings.presentation.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val getChatSettingsUseCase: GetChatSettingsUseCase,
    private val saveChatSettingsUseCase: SaveChatSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeChatSettings()
    }

    private fun observeChatSettings() {
        getChatSettingsUseCase()
            .onEach { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
            .launchIn(viewModelScope)
    }

    fun handleEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.ModelChanged -> {
                updateSettings { it.copy(selectedModel = event.model) }
            }
            is SettingsUiEvent.TemperatureChanged -> {
                updateSettings { it.copy(temperature = event.temperature) }
            }
            is SettingsUiEvent.MaxTokensChanged -> {
                updateSettings { it.copy(maxTokens = event.maxTokens) }
            }
            is SettingsUiEvent.SystemPromptChanged -> {
                updateSettings { it.copy(systemPrompt = event.systemPrompt) }
            }
            is SettingsUiEvent.SaveSettings -> {
                saveSettings()
            }
            is SettingsUiEvent.ResetToDefaults -> {
                resetToDefaults()
            }
        }
    }

    private fun updateSettings(update: (ChatSettings) -> ChatSettings) {
        val currentSettings = _uiState.value.settings
        val newSettings = update(currentSettings)
        _uiState.value = _uiState.value.copy(settings = newSettings)
    }

    private fun saveSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                saveChatSettingsUseCase(_uiState.value.settings)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to save settings"
                )
            }
        }
    }

    private fun resetToDefaults() {
        val defaultSettings = ChatSettings()
        _uiState.value = _uiState.value.copy(settings = defaultSettings)
        saveSettings()
    }
}