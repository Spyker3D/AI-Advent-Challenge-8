package com.aiassistant.core.data.client

import android.util.Log
import com.aiassistant.core.data.BuildConfig
import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.network.api.OpenAiApi
import com.aiassistant.core.network.api.OllamaApiFactory
import com.aiassistant.core.data.config.ApiConfig
import com.aiassistant.core.data.datastore.SettingsDataStore
import com.aiassistant.core.network.dto.OpenAiInputMessageDto
import com.aiassistant.core.network.dto.OpenAiResponseRequestDto
import com.aiassistant.core.network.dto.OllamaGenerateRequestDto
import com.aiassistant.core.network.dto.OllamaOptionsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class LlmClientImpl @Inject constructor(
    private val openAiApi: OpenAiApi,
    private val ollamaApiFactory: OllamaApiFactory,
    private val settingsDataStore: SettingsDataStore,
    private val apiConfig: ApiConfig
) : LlmClient {
    
    companion object {
        private const val OPENAI_TEMPERATURE = 0.2
        private const val OPENAI_MAX_OUTPUT_TOKENS = 1200
        private const val OPENAI_SYSTEM_PROMPT = """Ты AI Assistant. Отвечай на языке пользователя.
Давай точные и понятные ответы.
Не выдумывай факты. Если информации недостаточно, скажи об этом."""
        private const val OLLAMA_TEMPERATURE = 0.2
        private const val OLLAMA_NUM_CTX = 8192
        private const val TAG = "OpenAiRequest"
    }

    override suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String?): Result<ChatResponse> = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.chatSettings.first()
        when (settings.provider) {
            AiProvider.OPENAI -> sendViaOpenAi(messages, maxTokens, model, settings)
            AiProvider.LOCAL_OLLAMA -> sendViaOllama(messages)
        }
    }

    private suspend fun sendViaOpenAi(
        messages: List<Message>,
        maxTokens: Int?,
        model: String?,
        settings: ChatSettings
    ): Result<ChatResponse> {
        if (apiConfig.openAiApiKey.isBlank()) {
            return Result.failure(Exception("OpenAI API key не настроен.\nДобавьте OPENAI_API_KEY в local.properties и пересоберите приложение."))
        }
        return try {
            val modelName = ChatSettings.normalizeOpenAiModel(
                model?.takeIf { it.isNotBlank() } ?: settings.openAiModel
            )
            val systemContext = messages.filter { it.role == MessageRole.SYSTEM }
                .joinToString("\n\n") { it.content }.trim()
            val input = buildList {
                add(OpenAiInputMessageDto("system", listOf(OPENAI_SYSTEM_PROMPT, systemContext)
                    .filter { it.isNotBlank() }.joinToString("\n\n")))
                messages.filterNot { it.role == MessageRole.SYSTEM }.forEach {
                    add(OpenAiInputMessageDto(if (it.role == MessageRole.ASSISTANT) "assistant" else "user", it.content))
                }
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "OpenAI model: $modelName")
            val response = openAiApi.createResponse(OpenAiResponseRequestDto(
                model = modelName,
                input = input,
                temperature = OPENAI_TEMPERATURE.takeUnless { modelName.lowercase().startsWith("gpt-5") },
                maxOutputTokens = maxTokens ?: OPENAI_MAX_OUTPUT_TOKENS
            ))
            val text = response.outputText()
            if (text.isBlank()) Result.failure(Exception("OpenAI вернул пустой ответ."))
            else Result.success(ChatResponse(text, response.usage?.outputTokens))
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

    private fun openAiHttpError(code: Int): String = when (code) {
        400 -> "OpenAI отклонил запрос. Проверьте имя модели и параметры запроса."
        401 -> "Неверный OpenAI API key.\nПроверьте OPENAI_API_KEY в local.properties."
        403 -> "Доступ к OpenAI API запрещён для текущего аккаунта или сети."
        429 -> "Превышен лимит OpenAI API или закончился доступный баланс.\nПроверьте Billing и Usage в OpenAI Platform."
        in 500..599 -> "OpenAI временно недоступен. Повторите запрос позже."
        else -> "OpenAI отклонил запрос (HTTP $code)."
    }

    private suspend fun sendViaOllama(messages: List<Message>): Result<ChatResponse> {
        val settings = settingsDataStore.chatSettings.first()
        val baseUrl = settings.localBaseUrl.ifBlank { ChatSettings.DEFAULT_LOCAL_BASE_URL }
        val model = settings.localModel.ifBlank { ChatSettings.DEFAULT_LOCAL_MODEL }

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
                    stream = false,
                    options = OllamaOptionsDto(
                        temperature = OLLAMA_TEMPERATURE,
                        numCtx = OLLAMA_NUM_CTX
                    )
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
}
