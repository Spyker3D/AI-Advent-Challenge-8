package com.aiassistant.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aiassistant.core.domain.entity.AiProvider
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
        val PROVIDER = stringPreferencesKey("provider")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val LOCAL_BASE_URL = stringPreferencesKey("local_base_url")
        val LOCAL_MODEL = stringPreferencesKey("local_model")
        // Day 2 fields
        val USE_JSON_FORMAT = androidx.datastore.preferences.core.booleanPreferencesKey("use_json_format")
        val LIMIT_LENGTH = androidx.datastore.preferences.core.booleanPreferencesKey("limit_length")
        val USE_STOP_SEQUENCE = androidx.datastore.preferences.core.booleanPreferencesKey("use_stop_sequence")
        val STOP_SEQUENCE_TEXT = stringPreferencesKey("stop_sequence_text")
        // Context compression fields
        val USE_CONTEXT_COMPRESSION = androidx.datastore.preferences.core.booleanPreferencesKey("use_context_compression")
        val KEEP_LAST_MESSAGES_COUNT = intPreferencesKey("keep_last_messages_count")
    }

    val chatSettings: Flow<ChatSettings> = context.dataStore.data.map { preferences ->
        ChatSettings(
            provider = preferences[PreferencesKeys.PROVIDER]
                ?.let { runCatching { AiProvider.valueOf(it) }.getOrNull() }
                ?: AiProvider.OPENROUTER,
            selectedModel = AiModel.fromModelName(
                preferences[PreferencesKeys.SELECTED_MODEL] ?: AiModel.getDefault().modelName
            ) ?: AiModel.getDefault(),
            temperature = preferences[PreferencesKeys.TEMPERATURE] ?: 0.7f,
            maxTokens = preferences[PreferencesKeys.MAX_TOKENS] ?: 1000,
            systemPrompt = preferences[PreferencesKeys.SYSTEM_PROMPT] ?: "You are a helpful AI assistant.",
            localBaseUrl = preferences[PreferencesKeys.LOCAL_BASE_URL]
                ?: ChatSettings.DEFAULT_LOCAL_BASE_URL,
            localModel = preferences[PreferencesKeys.LOCAL_MODEL]
                ?: ChatSettings.DEFAULT_LOCAL_MODEL,
            // Day 2 fields
            useJsonFormat = preferences[PreferencesKeys.USE_JSON_FORMAT] ?: false,
            limitLength = preferences[PreferencesKeys.LIMIT_LENGTH] ?: false,
            useStopSequence = preferences[PreferencesKeys.USE_STOP_SEQUENCE] ?: false,
            stopSequenceText = preferences[PreferencesKeys.STOP_SEQUENCE_TEXT] ?: "",
            // Context compression fields
            useContextCompression = preferences[PreferencesKeys.USE_CONTEXT_COMPRESSION] ?: false,
            keepLastMessagesCount = preferences[PreferencesKeys.KEEP_LAST_MESSAGES_COUNT] ?: 6
        )
    }

    suspend fun saveChatSettings(settings: ChatSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROVIDER] = settings.provider.name
            preferences[PreferencesKeys.SELECTED_MODEL] = settings.selectedModel.modelName
            preferences[PreferencesKeys.TEMPERATURE] = settings.temperature
            preferences[PreferencesKeys.MAX_TOKENS] = settings.maxTokens
            preferences[PreferencesKeys.SYSTEM_PROMPT] = settings.systemPrompt
            preferences[PreferencesKeys.LOCAL_BASE_URL] = settings.localBaseUrl
            preferences[PreferencesKeys.LOCAL_MODEL] = settings.localModel
            // Day 2 fields
            preferences[PreferencesKeys.USE_JSON_FORMAT] = settings.useJsonFormat
            preferences[PreferencesKeys.LIMIT_LENGTH] = settings.limitLength
            preferences[PreferencesKeys.USE_STOP_SEQUENCE] = settings.useStopSequence
            preferences[PreferencesKeys.STOP_SEQUENCE_TEXT] = settings.stopSequenceText
            // Context compression fields
            preferences[PreferencesKeys.USE_CONTEXT_COMPRESSION] = settings.useContextCompression
            preferences[PreferencesKeys.KEEP_LAST_MESSAGES_COUNT] = settings.keepLastMessagesCount
        }
    }
}
