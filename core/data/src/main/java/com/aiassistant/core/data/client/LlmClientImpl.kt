package com.aiassistant.core.data.client

import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.network.api.OpenRouterApi
import com.aiassistant.core.network.api.OllamaApiFactory
import com.aiassistant.core.data.config.ApiConfig
import com.aiassistant.core.data.datastore.SettingsDataStore
import com.aiassistant.core.data.mapper.ChatMapper
import com.aiassistant.core.network.dto.MessageDto
import com.aiassistant.core.network.dto.OllamaGenerateRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class LlmClientImpl @Inject constructor(
    private val openRouterApi: OpenRouterApi,
    private val ollamaApiFactory: OllamaApiFactory,
    private val settingsDataStore: SettingsDataStore,
    private val chatMapper: ChatMapper,
    private val apiConfig: ApiConfig
) : LlmClient {
    
    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    override suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String?): Result<ChatResponse> = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.chatSettings.first()
        when (settings.provider) {
            AiProvider.OPENROUTER -> sendViaOpenRouter(messages, maxTokens, model)
            AiProvider.LOCAL_OLLAMA -> sendViaOllama(messages)
        }
    }

    private suspend fun sendViaOpenRouter(messages: List<Message>, maxTokens: Int?, model: String?): Result<ChatResponse> {
        return try {
            // Check if API key is configured
            val apiKey = apiConfig.openRouterApiKey
            if (apiKey.isBlank()) {
                return Result.failure(Exception("OpenRouter API key is not configured. Please add OPENROUTER_API_KEY to local.properties"))
            }
            
            // Map domain messages to DTOs
            val messageDtos = messages.map { message ->
                MessageDto(
                    role = message.role.value,
                    content = message.content
                )
            }
            
            // Determine model - use provided model or default to gpt-4o-mini
            val modelName = model ?: "gpt-4o-mini"
            
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

    private suspend fun sendViaOllama(messages: List<Message>): Result<ChatResponse> {
        val settings = settingsDataStore.chatSettings.first()
        val baseUrl = settings.localBaseUrl.ifBlank { "http://10.0.2.2:11434" }
        val model = settings.localModel.ifBlank { "llama3.2:3b" }

        return try {
            val systemPrompt = messages
                .firstOrNull { it.role == MessageRole.SYSTEM }
                ?.content
                ?.takeIf { it.isNotBlank() }
            val prompt = buildOllamaPrompt(messages.filterNot { it.role == MessageRole.SYSTEM })
            val response = ollamaApiFactory.create(baseUrl).generate(
                OllamaGenerateRequestDto(
                    model = model,
                    prompt = prompt,
                    system = systemPrompt,
                    stream = false
                )
            )
            val assistantMessage = response.response.trim()
            if (assistantMessage.isBlank()) {
                Result.failure(Exception("Empty response from local LLM"))
            } else {
                Result.success(ChatResponse(assistantMessage))
            }
        } catch (e: IllegalArgumentException) {
            Result.failure(Exception("Invalid Ollama Base URL: $baseUrl"))
        } catch (e: UnknownHostException) {
            Result.failure(Exception(localOllamaConnectionError(baseUrl)))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Local LLM request timed out. Check that Ollama is running and the model is responding."))
        } catch (e: HttpException) {
            val message = if (e.code() == 404) {
                "Ollama model '$model' was not found. Run: ollama pull $model"
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
}
