package com.aiassistant.core.domain.repository

import com.aiassistant.core.domain.entity.ChatSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getChatSettings(): Flow<ChatSettings>
    suspend fun saveChatSettings(settings: ChatSettings)
}