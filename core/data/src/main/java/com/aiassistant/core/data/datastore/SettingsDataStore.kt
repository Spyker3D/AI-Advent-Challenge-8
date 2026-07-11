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
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val LOCAL_BASE_URL = stringPreferencesKey("local_base_url")
        val LOCAL_MODEL = stringPreferencesKey("local_model")
        val LOCAL_TEMPERATURE = floatPreferencesKey("local_temperature")
        val LOCAL_MAX_TOKENS = intPreferencesKey("local_max_tokens")
        val LOCAL_CONTEXT_WINDOW = intPreferencesKey("local_context_window")
        val LOCAL_TOP_P = floatPreferencesKey("local_top_p")
        val LOCAL_REPEAT_PENALTY = floatPreferencesKey("local_repeat_penalty")
        val LOCAL_SEED = intPreferencesKey("local_seed")
        val LOCAL_SYSTEM_PROMPT = stringPreferencesKey("local_system_prompt")
        // Day 2 fields
        val USE_JSON_FORMAT = androidx.datastore.preferences.core.booleanPreferencesKey("use_json_format")
        val LIMIT_LENGTH = androidx.datastore.preferences.core.booleanPreferencesKey("limit_length")
        val USE_STOP_SEQUENCE = androidx.datastore.preferences.core.booleanPreferencesKey("use_stop_sequence")
        val STOP_SEQUENCE_TEXT = stringPreferencesKey("stop_sequence_text")
        // Context compression fields
        val USE_CONTEXT_COMPRESSION = androidx.datastore.preferences.core.booleanPreferencesKey("use_context_compression")
        val KEEP_LAST_MESSAGES_COUNT = intPreferencesKey("keep_last_messages_count")
    }

    private val legacyLocalModels = setOf("llama3.2" + ":3b")

    val chatSettings: Flow<ChatSettings> = context.dataStore.data.map { preferences ->
        ChatSettings(
            provider = migrateProvider(preferences[PreferencesKeys.PROVIDER]),
            selectedModel = AiModel.fromModelName(
                preferences[PreferencesKeys.SELECTED_MODEL] ?: AiModel.getDefault().modelName
            ) ?: AiModel.getDefault(),
            temperature = preferences[PreferencesKeys.TEMPERATURE] ?: 0.7f,
            maxTokens = preferences[PreferencesKeys.MAX_TOKENS] ?: 1000,
            systemPrompt = preferences[PreferencesKeys.SYSTEM_PROMPT] ?: "You are a helpful AI assistant.",
            openAiModel = ChatSettings.normalizeOpenAiModel(
                preferences[PreferencesKeys.OPENAI_MODEL]
            ),
            localBaseUrl = preferences[PreferencesKeys.LOCAL_BASE_URL]
                ?: ChatSettings.DEFAULT_LOCAL_BASE_URL,
            localModel = normalizeLocalModel(preferences[PreferencesKeys.LOCAL_MODEL]),
            localTemperature = ChatSettings.safeLocalTemperature(
                preferences[PreferencesKeys.LOCAL_TEMPERATURE] ?: ChatSettings.DEFAULT_LOCAL_TEMPERATURE
            ),
            localMaxTokens = ChatSettings.safeLocalMaxTokens(
                preferences[PreferencesKeys.LOCAL_MAX_TOKENS] ?: ChatSettings.DEFAULT_LOCAL_MAX_TOKENS
            ),
            localContextWindow = ChatSettings.safeLocalContextWindow(
                preferences[PreferencesKeys.LOCAL_CONTEXT_WINDOW] ?: ChatSettings.DEFAULT_LOCAL_CONTEXT_WINDOW
            ),
            localTopP = ChatSettings.safeLocalTopP(
                preferences[PreferencesKeys.LOCAL_TOP_P] ?: ChatSettings.DEFAULT_LOCAL_TOP_P
            ),
            localRepeatPenalty = ChatSettings.safeLocalRepeatPenalty(
                preferences[PreferencesKeys.LOCAL_REPEAT_PENALTY] ?: ChatSettings.DEFAULT_LOCAL_REPEAT_PENALTY
            ),
            localSeed = preferences[PreferencesKeys.LOCAL_SEED],
            localSystemPrompt = preferences[PreferencesKeys.LOCAL_SYSTEM_PROMPT]
                ?: ChatSettings.DEFAULT_LOCAL_SYSTEM_PROMPT,
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
            preferences[PreferencesKeys.OPENAI_MODEL] =
                ChatSettings.normalizeOpenAiModel(settings.openAiModel)
            preferences[PreferencesKeys.LOCAL_BASE_URL] = settings.localBaseUrl
            preferences[PreferencesKeys.LOCAL_MODEL] = normalizeLocalModel(settings.localModel)
            preferences[PreferencesKeys.LOCAL_TEMPERATURE] = ChatSettings.safeLocalTemperature(settings.localTemperature)
            preferences[PreferencesKeys.LOCAL_MAX_TOKENS] = ChatSettings.safeLocalMaxTokens(settings.localMaxTokens)
            preferences[PreferencesKeys.LOCAL_CONTEXT_WINDOW] = ChatSettings.safeLocalContextWindow(settings.localContextWindow)
            preferences[PreferencesKeys.LOCAL_TOP_P] = ChatSettings.safeLocalTopP(settings.localTopP)
            preferences[PreferencesKeys.LOCAL_REPEAT_PENALTY] = ChatSettings.safeLocalRepeatPenalty(settings.localRepeatPenalty)
            settings.localSeed?.let { preferences[PreferencesKeys.LOCAL_SEED] = it }
                ?: preferences.remove(PreferencesKeys.LOCAL_SEED)
            preferences[PreferencesKeys.LOCAL_SYSTEM_PROMPT] = settings.localSystemPrompt
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

    private fun normalizeLocalModel(localModel: String?): String {
        val normalized = localModel?.trim()
        return when {
            normalized.isNullOrEmpty() -> ChatSettings.DEFAULT_LOCAL_MODEL
            normalized in legacyLocalModels -> ChatSettings.DEFAULT_LOCAL_MODEL
            else -> normalized
        }
    }

    internal fun migrateProvider(value: String?): AiProvider = when (value?.uppercase()) {
        "LOCAL_OLLAMA", "LOCAL" -> AiProvider.LOCAL_OLLAMA
        "OPENROUTER", "ONLINE_OPENROUTER", "OPENAI", "ONLINE", null -> AiProvider.OPENAI
        else -> AiProvider.OPENAI
    }
}
