package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ClearConversationSummaryUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() {
        val settings = settingsRepository.getChatSettings().first()
        if (settings.conversationSummary.isNotEmpty()) {
            settingsRepository.saveChatSettings(settings.copy(conversationSummary = ""))
        }
    }
}