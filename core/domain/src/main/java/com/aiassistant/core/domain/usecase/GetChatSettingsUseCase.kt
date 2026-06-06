package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<ChatSettings> {
        return settingsRepository.getChatSettings()
    }
}