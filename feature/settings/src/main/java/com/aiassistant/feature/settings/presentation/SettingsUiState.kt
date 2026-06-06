package com.aiassistant.feature.settings.presentation

import com.aiassistant.core.domain.entity.AiModel
import com.aiassistant.core.domain.entity.ChatSettings

data class SettingsUiState(
    val settings: ChatSettings = ChatSettings(),
    val isLoading: Boolean = false,
    val error: String? = null
)