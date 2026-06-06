package com.aiassistant.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aiassistant.core.domain.entity.AiModel
import com.aiassistant.core.domain.entity.ChatSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chat_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private object PreferencesKeys {
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
    }

    val chatSettings: Flow<ChatSettings> = context.dataStore.data.map { preferences ->
        ChatSettings(
            selectedModel = AiModel.fromModelName(
                preferences[PreferencesKeys.SELECTED_MODEL] ?: AiModel.getDefault().modelName
            ) ?: AiModel.getDefault(),
            temperature = preferences[PreferencesKeys.TEMPERATURE] ?: 0.7f,
            maxTokens = preferences[PreferencesKeys.MAX_TOKENS] ?: 1000,
            systemPrompt = preferences[PreferencesKeys.SYSTEM_PROMPT] ?: "You are a helpful AI assistant."
        )
    }

    suspend fun saveChatSettings(settings: ChatSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_MODEL] = settings.selectedModel.modelName
            preferences[PreferencesKeys.TEMPERATURE] = settings.temperature
            preferences[PreferencesKeys.MAX_TOKENS] = settings.maxTokens
            preferences[PreferencesKeys.SYSTEM_PROMPT] = settings.systemPrompt
        }
    }
}