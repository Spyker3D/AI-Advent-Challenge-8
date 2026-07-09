package com.aiassistant.feature.settings.presentation

import com.aiassistant.core.domain.entity.AiModel
import com.aiassistant.core.domain.entity.AiProvider

sealed class SettingsUiEvent {
    data class ProviderChanged(val provider: AiProvider) : SettingsUiEvent()
    data class ModelChanged(val model: AiModel) : SettingsUiEvent()
    data class TemperatureChanged(val temperature: Float) : SettingsUiEvent()
    data class MaxTokensChanged(val maxTokens: Int) : SettingsUiEvent()
    data class SystemPromptChanged(val systemPrompt: String) : SettingsUiEvent()
    data class LocalBaseUrlChanged(val localBaseUrl: String) : SettingsUiEvent()
    data class LocalModelChanged(val localModel: String) : SettingsUiEvent()
    object SaveSettings : SettingsUiEvent()
    object ResetToDefaults : SettingsUiEvent()
    
    // Day 2 events
    data class UseJsonFormatChanged(val useJsonFormat: Boolean) : SettingsUiEvent()
    data class LimitLengthChanged(val limitLength: Boolean) : SettingsUiEvent()
    data class UseStopSequenceChanged(val useStopSequence: Boolean) : SettingsUiEvent()
    data class StopSequenceChanged(val stopSequenceText: String) : SettingsUiEvent()
    
    // Context compression events
    data class UseContextCompressionChanged(val useContextCompression: Boolean) : SettingsUiEvent()
    data class KeepLastMessagesCountChanged(val keepLastMessagesCount: Int) : SettingsUiEvent()
}
