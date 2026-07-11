package com.aiassistant.core.data.repository

import com.aiassistant.core.network.api.OpenRouterApi
import com.aiassistant.core.network.api.OllamaApiFactory
import com.aiassistant.core.data.config.ApiConfig
import com.aiassistant.core.data.database.ChatDatabase
import com.aiassistant.core.data.database.ChatMessageDao
import com.aiassistant.core.data.database.dao.ChatDao
import com.aiassistant.core.data.database.entity.ChatEntity
import com.aiassistant.core.data.datastore.SettingsDataStore
import com.aiassistant.core.data.mapper.ChatMapper
import com.aiassistant.core.data.mapper.ChatMessageMapper
import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.entity.AiChatResponse
import com.aiassistant.core.domain.entity.AiResponseMetadata
import com.aiassistant.core.domain.entity.Chat
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.entity.FormattedAiResponse
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.entity.TokenMetrics
import com.aiassistant.core.domain.util.TokenCounter
import com.aiassistant.core.domain.repository.ChatRepository
import com.aiassistant.core.network.dto.OllamaGenerateRequestDto
import com.aiassistant.core.network.dto.OllamaOptionsDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val openRouterApi: OpenRouterApi,
    private val ollamaApiFactory: OllamaApiFactory,
    private val settingsDataStore: SettingsDataStore,
    private val chatMapper: ChatMapper,
    private val chatMessageMapper: ChatMessageMapper,
    private val chatMessageDao: ChatMessageDao,
    private val chatDao: ChatDao,
    private val apiConfig: ApiConfig,
    private val gson: Gson
) : ChatRepository {
    
    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val OLLAMA_TEMPERATURE = 0.2
        private const val OLLAMA_NUM_CTX = 8192
    }
    

    
    private fun getActualCostFromBody(body: com.aiassistant.core.network.dto.ChatResponseDto?): Double? {
        // Try to get actual cost from the response body if available
        return body?.usage?.cost
    }

    override suspend fun sendMessage(chatRequest: ChatRequest): Result<AiChatResponse> {
        return withContext(Dispatchers.IO) {
            val settings = settingsDataStore.chatSettings.first()
            when (settings.provider) {
                AiProvider.OPENROUTER -> sendViaOpenRouter(chatRequest)
                AiProvider.LOCAL_OLLAMA -> sendViaOllama(chatRequest)
            }
        }
    }

    private suspend fun sendViaOpenRouter(chatRequest: ChatRequest): Result<AiChatResponse> {
            return try {
                // Check if API key is configured
                val apiKey = apiConfig.openRouterApiKey
                if (apiKey.isBlank()) {
                    return Result.failure(Exception("OpenRouter API key is not configured. Please add OPENROUTER_API_KEY to local.properties"))
                }
                // Use the history already provided in the chatRequest (for compression) or get all messages
                val effectiveChatRequest = if (chatRequest.history.isNotEmpty()) {
                    chatRequest
                } else {
                    chatRequest.copy(history = getMessages())
                }
                val requestDto = chatMapper.mapToChatRequestDto(effectiveChatRequest)
                val startTime = System.currentTimeMillis()
                val response = openRouterApi.sendChatMessage(
                    authorization = BEARER_PREFIX + apiKey,
                    request = requestDto
                )
                val responseTimeMs = System.currentTimeMillis() - startTime

                if (response.isSuccessful) {
                    val body = response.body()
                    when {
                        body?.error != null -> {
                            val error = body.error!!
                            Result.failure(Exception("API Error: ${error.message}"))
                        }
                        body?.choices?.isNotEmpty() == true -> {
                            val assistantMessage = body.choices.first().message.content
                            if (assistantMessage.isNotBlank()) {
                                // Create metadata
                                val metadata = AiResponseMetadata(
                                    modelDisplayName = chatRequest.model.displayName,
                                    modelApiName = chatRequest.model.modelName,
                                    responseTimeMs = responseTimeMs,
                                    promptTokens = body.usage?.promptTokens,
                                    completionTokens = body.usage?.completionTokens,
                                    totalTokens = body.usage?.totalTokens,
                                                                    estimatedCostUsd = getActualCostFromBody(body)
                                )
                                
                                Result.success(AiChatResponse(assistantMessage, metadata))
                            } else {
                                Result.failure(Exception("Empty response from AI model"))
                            }
                        }
                        else -> {
                            Result.failure(Exception("Invalid response format"))
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("HTTP ${response.code()}: ${errorBody ?: "Unknown error"}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
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
                    system = (effectiveChatRequest.systemPrompt ?: settings.systemPrompt)
                        .takeIf { it.isNotBlank() },
                    stream = false,
                    options = OllamaOptionsDto(
                        temperature = OLLAMA_TEMPERATURE,
                        numCtx = OLLAMA_NUM_CTX
                    )
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
                            estimatedCostUsd = null
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
        return "Модель $model не найдена в Ollama.\nУстановите её командой:\nollama pull $model"
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
