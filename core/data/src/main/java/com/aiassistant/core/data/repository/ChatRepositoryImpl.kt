package com.aiassistant.core.data.repository

import com.aiassistant.core.network.api.OpenRouterApi
import com.aiassistant.core.data.config.ApiConfig
import com.aiassistant.core.data.mapper.ChatMapper
import com.aiassistant.core.domain.entity.AiChatResponse
import com.aiassistant.core.domain.entity.AiResponseMetadata
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.entity.FormattedAiResponse
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.repository.ChatRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val openRouterApi: OpenRouterApi,
    private val chatMapper: ChatMapper,
    private val apiConfig: ApiConfig,
    private val gson: Gson
) : ChatRepository {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
    
    private val messages = mutableListOf<Message>()
    private val mutex = Mutex()
    
    private fun getActualCostFromBody(body: com.aiassistant.core.network.dto.ChatResponseDto?): Double? {
        // Try to get actual cost from the response body if available
        return body?.usage?.cost
    }

    override suspend fun sendMessage(chatRequest: ChatRequest): Result<AiChatResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if API key is configured
                val apiKey = apiConfig.openRouterApiKey
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("OpenRouter API key is not configured. Please add OPENROUTER_API_KEY to local.properties"))
                }
                val requestDto = chatMapper.mapToChatRequestDto(chatRequest)
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
    }

    override suspend fun sendMessageWithRestrictions(
        chatRequest: ChatRequest,
        useJsonFormat: Boolean,
        limitLength: Boolean,
        useStopSequence: Boolean,
        stopSequenceText: String
    ): Result<AiChatResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if API key is configured
                val apiKey = apiConfig.openRouterApiKey
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("OpenRouter API key is not configured. Please add OPENROUTER_API_KEY to local.properties"))
                }
                val requestDto = chatMapper.mapToChatRequestDtoWithRestrictions(
                    chatRequest = chatRequest,
                    useJsonFormat = useJsonFormat,
                    limitLength = limitLength,
                    useStopSequence = useStopSequence,
                    stopSequenceText = stopSequenceText
                )
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
    }

    override fun parseFormattedResponse(response: String): Result<FormattedAiResponse> {
        return try {
            val formattedResponse = gson.fromJson(response, FormattedAiResponse::class.java)
            Result.success(formattedResponse)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse JSON response: ${e.message}"))
        }
    }
    
    override suspend fun saveMessage(message: Message): Unit = mutex.withLock {
        messages.add(message)
    }
    
    override suspend fun getMessages(): List<Message> = mutex.withLock {
        messages.toList()
    }
    
    override suspend fun clearMessages(): Unit = mutex.withLock {
        messages.clear()
    }
}