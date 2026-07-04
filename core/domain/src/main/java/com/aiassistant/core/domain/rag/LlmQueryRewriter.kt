package com.aiassistant.core.domain.rag

import android.util.Log
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import java.util.UUID
import javax.inject.Inject

class LlmQueryRewriter @Inject constructor(
    private val llmClient: LlmClient
) : QueryRewriter {

    override suspend fun rewrite(question: String): String {
        val prompt = """
            |Ты переписываешь пользовательский вопрос для поиска по технической базе знаний Android-проекта.
            |Верни только поисковый запрос без объяснений.
            |Если вопрос на русском, добавь важные английские технические термины.
            |Не меняй смысл вопроса.
            |
            |Примеры:
            |Вопрос:
            |Как устроен поток отправки сообщения?
            |
            |Ответ:
            |message sending flow Chat Module ChatScreen ChatViewModel ChatRepository HTTP Request LLM Streaming Response Compose UI
            |
            |Вопрос:
            |Чем remember отличается от rememberSaveable?
            |
            |Ответ:
            |Jetpack Compose remember rememberSaveable state recomposition configuration changes Activity recreation
            |
            |Вопрос:
            |$question
            |
            |Ответ:
        """.trimMargin()

        val message = Message(
            id = UUID.randomUUID().toString(),
            content = prompt,
            role = MessageRole.USER
        )

        return llmClient.sendChat(
            messages = listOf(message),
            maxTokens = 120,
            model = "openai/gpt-4o-mini"
        ).map { response ->
            response.message
                .replace("```", "")
                .trim()
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.trim()
                .orEmpty()
        }.getOrElse { throwable ->
            Log.d("RAG_DAY23", "rewrite error=${throwable.message}")
            question
        }.ifBlank { question }
    }
}
