package com.aiassistant.core.data.client

import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.network.api.OpenRouterApi
import com.aiassistant.core.data.config.ApiConfig
import com.aiassistant.core.data.mapper.ChatMapper
import com.aiassistant.core.network.dto.MessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LlmClientImpl @Inject constructor(
    private val openRouterApi: OpenRouterApi,
    private val chatMapper: ChatMapper,
    private val apiConfig: ApiConfig
) : LlmClient {
    
    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    override suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String?): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            // Check if API key is configured
            val apiKey = apiConfig.openRouterApiKey
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("OpenRouter API key is not configured. Please add OPENROUTER_API_KEY to local.properties"))
            }
            
            // Map domain messages to DTOs
            val messageDtos = messages.map { message ->
                MessageDto(
                    role = message.role.value,
                    content = message.content
                )
            }
            
            // Determine model - use provided model or default to gpt-4.1-mini
            val modelName = model ?: "gpt-4.1-mini"
            
            // Create request DTO with proper parameters
            val requestDto = com.aiassistant.core.network.dto.ChatRequestDto(
                model = modelName,
                messages = messageDtos,
                temperature = 0.7f,
                maxTokens = maxTokens // Use the passed maxTokens parameter
            )
            
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
                            // Extract completion tokens from the response
                            val completionTokens = body.usage?.completionTokens
                            Result.success(ChatResponse(assistantMessage, completionTokens))
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