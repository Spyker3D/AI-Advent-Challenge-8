package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.repository.SettingsRepository
import javax.inject.Inject

class SaveChatSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(settings: ChatSettings) {
        settingsRepository.saveChatSettings(settings)
    }
}