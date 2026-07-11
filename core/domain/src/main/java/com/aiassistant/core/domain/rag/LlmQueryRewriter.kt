package com.aiassistant.core.domain.rag

import android.util.Log
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.memory.TaskContext
import java.util.UUID
import javax.inject.Inject

class LlmQueryRewriter @Inject constructor(
    private val llmClient: LlmClient,
    private val recentHistoryFormatter: RecentHistoryFormatter,
    private val taskMemoryPromptFormatter: TaskMemoryPromptFormatter
) : QueryRewriter {

    override suspend fun rewrite(question: String): String {
        return rewrite(
            question = question,
            taskContext = null,
            recentMessages = emptyList()
        )
    }

    override suspend fun rewrite(
        question: String,
        taskContext: TaskContext?,
        recentMessages: List<Message>
    ): String {
        val taskMemory = taskMemoryPromptFormatter.format(taskContext)
        val recentConversation = recentHistoryFormatter.format(
            messages = recentMessages,
            maxMessages = 4,
            maxChars = 1200
        )
        val prompt = """
            |Ты переписываешь пользовательский вопрос для поиска по технической базе знаний Android-проекта.
            |Верни только поисковый запрос без объяснений.
            |Если вопрос на русском, добавь важные английские технические термины.
            |Не меняй смысл вопроса.
            |Учитывай Task Memory и последние сообщения только для разрешения местоимений и уточнений.
            |Не добавляй факты, которые не помогают поиску по Android AI Assistant.
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
            |Task Memory:
            |$taskMemory
            |
            |Recent conversation:
            |$recentConversation
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
            model = ChatSettings.DEFAULT_OPENAI_MODEL
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
