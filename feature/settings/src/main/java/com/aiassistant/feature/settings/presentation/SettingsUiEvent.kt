package com.aiassistant.feature.settings.presentation

import com.aiassistant.core.domain.entity.AiModel

sealed class SettingsUiEvent {
    data class ModelChanged(val model: AiModel) : SettingsUiEvent()
    data class TemperatureChanged(val temperature: Float) : SettingsUiEvent()
    data class MaxTokensChanged(val maxTokens: Int) : SettingsUiEvent()
    data class SystemPromptChanged(val systemPrompt: String) : SettingsUiEvent()
    object SaveSettings : SettingsUiEvent()
    object ResetToDefaults : SettingsUiEvent()
}