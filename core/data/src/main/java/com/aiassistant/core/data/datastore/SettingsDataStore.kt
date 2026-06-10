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
        // Day 2 fields
        val USE_JSON_FORMAT = androidx.datastore.preferences.core.booleanPreferencesKey("use_json_format")
        val LIMIT_LENGTH = androidx.datastore.preferences.core.booleanPreferencesKey("limit_length")
        val USE_STOP_SEQUENCE = androidx.datastore.preferences.core.booleanPreferencesKey("use_stop_sequence")
        val STOP_SEQUENCE_TEXT = stringPreferencesKey("stop_sequence_text")
    }

    val chatSettings: Flow<ChatSettings> = context.dataStore.data.map { preferences ->
        ChatSettings(
            selectedModel = AiModel.fromModelName(
                preferences[PreferencesKeys.SELECTED_MODEL] ?: AiModel.getDefault().modelName
            ) ?: AiModel.getDefault(),
            temperature = preferences[PreferencesKeys.TEMPERATURE] ?: 0.7f,
            maxTokens = preferences[PreferencesKeys.MAX_TOKENS] ?: 1000,
            systemPrompt = preferences[PreferencesKeys.SYSTEM_PROMPT] ?: "You are a helpful AI assistant.",
            // Day 2 fields
            useJsonFormat = preferences[PreferencesKeys.USE_JSON_FORMAT] ?: false,
            limitLength = preferences[PreferencesKeys.LIMIT_LENGTH] ?: false,
            useStopSequence = preferences[PreferencesKeys.USE_STOP_SEQUENCE] ?: false,
            stopSequenceText = preferences[PreferencesKeys.STOP_SEQUENCE_TEXT] ?: ""
        )
    }

    suspend fun saveChatSettings(settings: ChatSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_MODEL] = settings.selectedModel.modelName
            preferences[PreferencesKeys.TEMPERATURE] = settings.temperature
            preferences[PreferencesKeys.MAX_TOKENS] = settings.maxTokens
            preferences[PreferencesKeys.SYSTEM_PROMPT] = settings.systemPrompt
            // Day 2 fields
            preferences[PreferencesKeys.USE_JSON_FORMAT] = settings.useJsonFormat
            preferences[PreferencesKeys.LIMIT_LENGTH] = settings.limitLength
            preferences[PreferencesKeys.USE_STOP_SEQUENCE] = settings.useStopSequence
            preferences[PreferencesKeys.STOP_SEQUENCE_TEXT] = settings.stopSequenceText
        }
    }
}