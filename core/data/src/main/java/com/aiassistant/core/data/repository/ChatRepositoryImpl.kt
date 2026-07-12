package com.aiassistant.core.data.repository

import android.util.Log
import com.aiassistant.core.data.BuildConfig
import com.aiassistant.core.network.api.OpenAiApi
import com.aiassistant.core.network.api.OllamaApiFactory
import com.aiassistant.core.network.api.PrivateVpsApi
import com.aiassistant.core.network.interceptor.PrivateVpsCredentials
import com.aiassistant.core.data.config.ApiConfig
import com.aiassistant.core.data.database.ChatDatabase
import com.aiassistant.core.data.database.ChatMessageDao
import com.aiassistant.core.data.database.dao.ChatDao
import com.aiassistant.core.data.database.entity.ChatEntity
import com.aiassistant.core.data.datastore.SettingsDataStore
import com.aiassistant.core.data.mapper.ChatMessageMapper
import com.aiassistant.core.data.mapper.toOllamaOptionsDto
import com.aiassistant.core.data.mapper.buildPrivateVpsRequest
import com.aiassistant.core.data.mapper.privateVpsEndpoint
import com.aiassistant.core.data.mapper.privateVpsHttpError
import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.entity.AiChatResponse
import com.aiassistant.core.domain.entity.AiResponseMetadata
import com.aiassistant.core.domain.entity.Chat
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.entity.FormattedAiResponse
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.entity.LocalGenerationMetrics
import com.aiassistant.core.domain.entity.TokenMetrics
import com.aiassistant.core.domain.util.TokenCounter
import com.aiassistant.core.domain.repository.ChatRepository
import com.aiassistant.core.network.dto.OllamaGenerateRequestDto
import com.aiassistant.core.network.dto.OllamaOptionsDto
import com.aiassistant.core.network.dto.OpenAiInputMessageDto
import com.aiassistant.core.network.dto.OpenAiResponseRequestDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val openAiApi: OpenAiApi,
    private val privateVpsApi: PrivateVpsApi,
    private val privateVpsCredentials: PrivateVpsCredentials,
    private val ollamaApiFactory: OllamaApiFactory,
    private val settingsDataStore: SettingsDataStore,
    private val chatMessageMapper: ChatMessageMapper,
    private val chatMessageDao: ChatMessageDao,
    private val chatDao: ChatDao,
    private val apiConfig: ApiConfig,
    private val gson: Gson
) : ChatRepository {
    
    companion object {
        private const val OPENAI_TEMPERATURE = 0.2
        private const val OPENAI_MAX_OUTPUT_TOKENS = 1200
        private const val OPENAI_SYSTEM_PROMPT = """Ты AI Assistant. Отвечай на языке пользователя.
Давай точные и понятные ответы.
Не выдумывай факты. Если информации недостаточно, скажи об этом."""
        private const val TAG = "OpenAiRequest"
    }
    

    
    override suspend fun sendMessage(chatRequest: ChatRequest): Result<AiChatResponse> {
        return withContext(Dispatchers.IO) {
            val settings = settingsDataStore.chatSettings.first()
            when (settings.provider) {
                AiProvider.OPENAI -> sendViaOpenAi(chatRequest, settings)
                AiProvider.LOCAL_OLLAMA -> sendViaOllama(chatRequest)
                AiProvider.PRIVATE_VPS -> sendViaPrivateVps(chatRequest, settings)
            }
        }
    }

    private suspend fun sendViaOpenAi(
        chatRequest: ChatRequest,
        settings: ChatSettings
    ): Result<AiChatResponse> {
        if (apiConfig.openAiApiKey.isBlank()) {
            return Result.failure(Exception("OpenAI API key не настроен.\nДобавьте OPENAI_API_KEY в local.properties и пересоберите приложение."))
        }
        val model = ChatSettings.normalizeOpenAiModel(settings.openAiModel)
        val effectiveHistory = if (chatRequest.history.isNotEmpty()) chatRequest.history else getMessages()
        val input = buildList {
            add(OpenAiInputMessageDto("system", OPENAI_SYSTEM_PROMPT))
            effectiveHistory.filterNot { it.role == MessageRole.SYSTEM }.forEach { message ->
                add(OpenAiInputMessageDto(message.role.toOpenAiRole(), message.content))
            }
            add(OpenAiInputMessageDto("user", chatRequest.message))
        }
        return try {
            val startTime = System.currentTimeMillis()
            if (BuildConfig.DEBUG) Log.d(TAG, "OpenAI model: $model")
            val response = openAiApi.createResponse(
                OpenAiResponseRequestDto(
                    model = model,
                    input = input,
                    temperature = OPENAI_TEMPERATURE.takeIf { supportsTemperature(model) },
                    maxOutputTokens = OPENAI_MAX_OUTPUT_TOKENS
                )
            )
            val text = response.outputText()
            if (text.isBlank()) return Result.failure(Exception("OpenAI вернул пустой ответ."))
            Result.success(AiChatResponse(text, AiResponseMetadata(
                modelDisplayName = model,
                modelApiName = response.model ?: model,
                responseTimeMs = System.currentTimeMillis() - startTime,
                promptTokens = response.usage?.inputTokens,
                completionTokens = response.usage?.outputTokens,
                totalTokens = response.usage?.totalTokens,
                estimatedCostUsd = null
            )))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("OpenAI не ответил вовремя. Проверьте интернет или VPN."))
        } catch (e: UnknownHostException) {
            Result.failure(Exception("Нет подключения к интернету."))
        } catch (e: HttpException) {
            Result.failure(Exception(openAiHttpError(e.code())))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Нет подключения к интернету."))
        } catch (e: Exception) {
            Result.failure(Exception("Не удалось получить ответ OpenAI."))
        }
    }

    private fun MessageRole.toOpenAiRole(): String = when (this) {
        MessageRole.USER -> "user"
        MessageRole.ASSISTANT -> "assistant"
        MessageRole.SYSTEM -> "system"
    }

    private fun supportsTemperature(model: String): Boolean {
        val normalized = model.lowercase()
        return !normalized.startsWith("gpt-5") && !normalized.startsWith("o1") &&
            !normalized.startsWith("o3") && !normalized.startsWith("o4")
    }

    private fun openAiHttpError(code: Int): String = when (code) {
        400 -> "OpenAI отклонил запрос. Проверьте имя модели и параметры запроса."
        401 -> "Неверный OpenAI API key.\nПроверьте OPENAI_API_KEY в local.properties."
        403 -> "Доступ к OpenAI API запрещён для текущего аккаунта или сети."
        429 -> "Превышен лимит OpenAI API или закончился доступный баланс.\nПроверьте Billing и Usage в OpenAI Platform."
        in 500..599 -> "OpenAI временно недоступен. Повторите запрос позже."
        else -> "OpenAI отклонил запрос (HTTP $code)."
    }

    private suspend fun sendViaOllama(chatRequest: ChatRequest): Result<AiChatResponse> {
        val settings = settingsDataStore.chatSettings.first()
        val baseUrl = settings.localBaseUrl.ifBlank { ChatSettings.DEFAULT_LOCAL_BASE_URL }
        val model = settings.localModel.ifBlank { ChatSettings.DEFAULT_LOCAL_MODEL }
        val effectiveChatRequest = if (chatRequest.history.isNotEmpty()) {
            chatRequest
        } else {
            chatRequest.copy(history = getMessages())
        }
        val messages = effectiveChatRequest.history + Message(
            id = java.util.UUID.randomUUID().toString(),
            content = effectiveChatRequest.message,
            role = MessageRole.USER
        )

        return try {
            val startTime = System.currentTimeMillis()
            val response = ollamaApiFactory.create(baseUrl).generate(
                OllamaGenerateRequestDto(
                    model = model,
                    prompt = buildOllamaPrompt(messages.filterNot { it.role == MessageRole.SYSTEM }),
                    system = settings.localSystemPrompt.takeIf { it.isNotBlank() },
                    stream = false,
                    options = settings.toOllamaOptionsDto()
                )
            )
            val responseTimeMs = System.currentTimeMillis() - startTime
            val assistantMessage = response.response.trim()

            if (assistantMessage.isBlank()) {
                Result.failure(Exception("Empty response from local LLM"))
            } else {
                Result.success(
                    AiChatResponse(
                        message = assistantMessage,
                        metadata = AiResponseMetadata(
                            modelDisplayName = model,
                            modelApiName = model,
                            responseTimeMs = responseTimeMs,
                            promptTokens = null,
                            completionTokens = null,
                            totalTokens = null,
                            estimatedCostUsd = null,
                            localMetrics = LocalGenerationMetrics(
                                model = model,
                                temperature = settings.localTemperature,
                                maxOutputTokens = settings.localMaxTokens,
                                contextWindow = settings.localContextWindow,
                                topP = settings.localTopP,
                                repeatPenalty = settings.localRepeatPenalty,
                                seed = settings.localSeed,
                                promptTokens = response.promptEvalCount,
                                outputTokens = response.evalCount,
                                totalDurationNanos = response.totalDuration,
                                loadDurationNanos = response.loadDuration,
                                evalDurationNanos = response.evalDuration
                            )
                        )
                    )
                )
            }
        } catch (e: IllegalArgumentException) {
            Result.failure(Exception("Invalid Ollama Base URL: $baseUrl"))
        } catch (e: UnknownHostException) {
            Result.failure(Exception(localOllamaConnectionError(baseUrl)))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Local LLM request timed out. Check that Ollama is running and the model is responding."))
        } catch (e: HttpException) {
            val message = if (e.code() == 404) {
                ollamaModelNotFoundMessage(model)
            } else {
                "Ollama HTTP ${e.code()}: ${e.message()}"
            }
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(Exception(localOllamaConnectionError(baseUrl), e))
        }
    }

    private suspend fun sendViaPrivateVps(chatRequest: ChatRequest, settings: ChatSettings): Result<AiChatResponse> {
        val url = settings.privateVpsEndpoint("api/chat/completions")
            ?: return Result.failure(Exception("VPS URL не настроен или некорректен."))
        if (settings.privateVpsApiKey.isBlank()) return Result.failure(Exception("API key приватного VPS не настроен."))
        if (settings.privateVpsModel.isBlank()) return Result.failure(Exception("VPS model не настроена."))
        privateVpsCredentials.apiKey = settings.privateVpsApiKey
        val history = (if (chatRequest.history.isNotEmpty()) chatRequest.history else getMessages()) + Message(
            java.util.UUID.randomUUID().toString(), chatRequest.message, MessageRole.USER
        )
        return try {
            val started = System.currentTimeMillis()
            val response = privateVpsApi.createChatCompletion(url, buildPrivateVpsRequest(history, settings, chatRequest.maxTokens))
            val text = response.outputText()
            if (text.isBlank()) return Result.failure(Exception("VPS вернул пустой ответ."))
            val usage = response.usage
            Result.success(AiChatResponse(text, AiResponseMetadata(
                "VPS · ${response.model ?: settings.privateVpsModel}", response.model ?: settings.privateVpsModel,
                System.currentTimeMillis() - started, usage?.effectiveInputTokens(), usage?.effectiveOutputTokens(),
                usage?.totalTokens, null,
                tokensPerSecond = usage?.responseTokensPerSecond
            )))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("VPS-модель не ответила вовремя."))
        } catch (e: UnknownHostException) {
            Result.failure(Exception("Не удалось подключиться к VPS. Проверьте адрес и доступность сервера."))
        } catch (e: HttpException) {
            Result.failure(Exception(privateVpsHttpError(e.code(), e.response()?.headers()?.get("Retry-After"))))
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("Не удалось подключиться к VPS. Проверьте адрес и доступность сервера."))
        } catch (e: java.io.IOException) {
            val message = if (e.message.orEmpty().contains("CLEARTEXT", true))
                "Android заблокировал HTTP-подключение. Используйте HTTPS или debug-конфигурацию."
            else "Не удалось подключиться к VPS. Проверьте адрес и доступность сервера."
            Result.failure(Exception(message))
        }
    }

    private fun buildOllamaPrompt(messages: List<Message>): String {
        return messages.joinToString("\n") { message ->
            when (message.role) {
                MessageRole.USER -> "User: ${message.content}"
                MessageRole.ASSISTANT -> "Assistant: ${message.content}"
                MessageRole.SYSTEM -> "System: ${message.content}"
            }
        }
    }

    private fun localOllamaConnectionError(baseUrl: String): String {
        return "Не удалось подключиться к локальной LLM.\nПроверь, что Ollama запущена и доступна по $baseUrl"
    }

    private fun ollamaModelNotFoundMessage(model: String): String {
        return "Модель $model не установлена.\nВыполните:\nollama pull $model"
    }

    // This method is now handled by the ChatAgent
    override suspend fun sendMessageWithRestrictions(
        chatRequest: ChatRequest,
        useJsonFormat: Boolean,
        limitLength: Boolean,
        useStopSequence: Boolean,
        stopSequenceText: String
    ): Result<AiChatResponse> {
        return Result.failure(Exception("This method is deprecated. Use ChatAgent.sendMessageWithRestrictions instead."))
    }

    override fun parseFormattedResponse(response: String): Result<FormattedAiResponse> {
        return try {
            val formattedResponse = gson.fromJson(response, FormattedAiResponse::class.java)
            Result.success(formattedResponse)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse JSON response: ${e.message}"))
        }
    }
    
    override suspend fun saveMessage(message: Message, branchId: String) {
        withContext(Dispatchers.IO) {
            chatMessageDao.insertMessage(chatMessageMapper.toEntity(message, branchId))
        }
    }
    
    override suspend fun getMessages(branchId: String): List<Message> {
        return withContext(Dispatchers.IO) {
            chatMessageDao.getMessages(branchId).map { chatMessageMapper.toDomain(it) }
        }
    }
    
    override suspend fun clearMessages(branchId: String) {
        withContext(Dispatchers.IO) {
            chatMessageDao.clearMessages(branchId)
        }
    }
    
    // Chat management methods
    override suspend fun getChats(): List<Chat> {
        return withContext(Dispatchers.IO) {
            chatDao.getChats().map {
                Chat(
                    id = it.id,
                    title = it.title,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    lastMessagePreview = it.lastMessagePreview,
                    activeTaskContextId = it.activeTaskContextId
                )
            }
        }
    }
    
    override suspend fun createChat(title: String): Chat {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val chatId = java.util.UUID.randomUUID().toString()
            val chatEntity = ChatEntity(chatId, title, now, now, "")
            chatDao.insertChat(chatEntity)
            Chat(chatId, title, now, now, "")
        }
    }
    
    override suspend fun deleteChat(chatId: String) {
        withContext(Dispatchers.IO) {
            chatDao.deleteChat(chatId)
            chatMessageDao.clearMessages(chatId)
        }
    }
    
    override suspend fun updateChatMeta(chatId: String, title: String, preview: String) {
        withContext(Dispatchers.IO) {
            chatDao.updateChatMeta(chatId, title, System.currentTimeMillis(), preview)
        }
    }

    override suspend fun updateChatActiveTaskContext(chatId: String, taskContextId: String?) {
        withContext(Dispatchers.IO) {
            chatDao.updateChatActiveTaskContext(chatId, taskContextId, System.currentTimeMillis())
        }
    }
}
