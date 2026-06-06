package com.aiassistant.core.data.repository

import com.aiassistant.core.data.datastore.SettingsDataStore
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override fun getChatSettings(): Flow<ChatSettings> {
        return settingsDataStore.chatSettings
    }

    override suspend fun saveChatSettings(settings: ChatSettings) {
        settingsDataStore.saveChatSettings(settings)
    }
}