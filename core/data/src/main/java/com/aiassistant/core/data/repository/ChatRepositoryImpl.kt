package com.aiassistant.core.data.repository

import com.aiassistant.core.network.api.OpenRouterApi
import com.aiassistant.core.data.config.ApiConfig
import com.aiassistant.core.data.mapper.ChatMapper
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val openRouterApi: OpenRouterApi,
    private val chatMapper: ChatMapper,
    private val apiConfig: ApiConfig
) : ChatRepository {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    override suspend fun sendMessage(chatRequest: ChatRequest): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if API key is configured
                val apiKey = apiConfig.openRouterApiKey
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("OpenRouter API key is not configured. Please add OPENROUTER_API_KEY to local.properties"))
                }
                val requestDto = chatMapper.mapToChatRequestDto(chatRequest)
                val response = openRouterApi.sendChatMessage(
                    authorization = BEARER_PREFIX + apiKey,
                    request = requestDto
                )

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
                                Result.success(assistantMessage)
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
}