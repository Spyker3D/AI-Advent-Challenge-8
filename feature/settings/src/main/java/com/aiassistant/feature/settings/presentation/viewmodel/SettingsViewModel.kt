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
            is SettingsUiEvent.ProviderChanged -> {
                updateSettings { it.copy(provider = event.provider) }
                saveSettings()
            }
            is SettingsUiEvent.ModelChanged -> {
                updateSettings { it.copy(selectedModel = event.model) }
                saveSettings()
            }
            is SettingsUiEvent.TemperatureChanged -> {
                updateSettings { it.copy(temperature = event.temperature) }
                saveSettings()
            }
            is SettingsUiEvent.MaxTokensChanged -> {
                updateSettings { it.copy(maxTokens = event.maxTokens) }
                saveSettings()
            }
            is SettingsUiEvent.SystemPromptChanged -> {
                updateSettings { it.copy(systemPrompt = event.systemPrompt) }
                saveSettings()
            }
            is SettingsUiEvent.LocalBaseUrlChanged -> {
                updateSettings { it.copy(localBaseUrl = event.localBaseUrl) }
                saveSettings()
            }
            is SettingsUiEvent.LocalModelChanged -> {
                updateSettings { it.copy(localModel = event.localModel) }
                saveSettings()
            }
            is SettingsUiEvent.OpenAiModelChanged -> {
                updateSettings { it.copy(openAiModel = event.openAiModel) }
                saveSettings()
            }
            is SettingsUiEvent.SaveSettings -> {
                saveSettings()
            }
            is SettingsUiEvent.ResetToDefaults -> {
                resetToDefaults()
            }
            // Day 2 events
            is SettingsUiEvent.UseJsonFormatChanged -> {
                updateSettings { it.copy(useJsonFormat = event.useJsonFormat) }
                saveSettings()
            }
            is SettingsUiEvent.LimitLengthChanged -> {
                updateSettings { it.copy(limitLength = event.limitLength) }
                saveSettings()
            }
            is SettingsUiEvent.UseStopSequenceChanged -> {
                updateSettings { it.copy(useStopSequence = event.useStopSequence) }
                saveSettings()
            }
            is SettingsUiEvent.StopSequenceChanged -> {
                updateSettings { it.copy(stopSequenceText = event.stopSequenceText) }
                saveSettings()
            }
            // Context compression events
            is SettingsUiEvent.UseContextCompressionChanged -> {
                updateSettings { it.copy(useContextCompression = event.useContextCompression) }
                saveSettings()
            }
            is SettingsUiEvent.KeepLastMessagesCountChanged -> {
                updateSettings { it.copy(keepLastMessagesCount = event.keepLastMessagesCount) }
                saveSettings()
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
