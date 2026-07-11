package com.aiassistant.core.domain.memory

import android.util.Log
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.rag.RecentHistoryFormatter
import com.aiassistant.core.domain.rag.TaskMemoryPromptFormatter
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import java.util.UUID
import javax.inject.Inject

class LlmTaskMemoryUpdater @Inject constructor(
    private val llmClient: LlmClient,
    private val gson: Gson,
    private val recentHistoryFormatter: RecentHistoryFormatter,
    private val taskMemoryPromptFormatter: TaskMemoryPromptFormatter
) : TaskMemoryUpdater {

    override suspend fun updateFromConversation(
        taskContext: TaskContext,
        recentMessages: List<Message>
    ): TaskContextUpdate {
        val prompt = """
            |Ты обновляешь память задачи Android AI Assistant.
            |Не отвечай пользователю.
            |Извлеки только устойчивую информацию, полезную для продолжения диалога.
            |
            |Сохраняй:
            |- цель пользователя;
            |- ограничения;
            |- принятые решения;
            |- уточнения;
            |- важные термины;
            |- текущее состояние задачи.
            |
            |Не сохраняй:
            |- короткие реакции;
            |- случайные фразы;
            |- повторы;
            |- обычные вопросы без нового факта;
            |- временные ошибки, если они уже не важны для дальнейшей работы.
            |
            |Текущая память:
            |${taskMemoryPromptFormatter.format(taskContext)}
            |
            |Последние сообщения:
            |${recentHistoryFormatter.format(recentMessages, maxMessages = 8, maxChars = 3000)}
            |
            |Верни только валидный JSON:
            |
            |{
            |  "goals_add": [],
            |  "constraints_add": [],
            |  "decisions_add": [],
            |  "clarifications_add": [],
            |  "terms_add": [],
            |  "current_state": null
            |}
            |
            |Если нечего сохранять - вернуть пустые массивы и current_state = null.
        """.trimMargin()

        val response = llmClient.sendChat(
            messages = listOf(
                Message(
                    id = UUID.randomUUID().toString(),
                    content = prompt,
                    role = MessageRole.USER
                )
            ),
            maxTokens = 500,
            model = ChatSettings.DEFAULT_OPENAI_MODEL
        ).getOrElse { throwable ->
            Log.d(LOG_TAG, "TASK_MEMORY_UPDATE_ERROR=${throwable.message}")
            return TaskContextUpdate()
        }

        return try {
            val json = response.message.extractJsonObject()
            Log.d(LOG_TAG, "TASK_MEMORY_UPDATE_JSON=${json.preview(1200)}")
            val jsonObject = JsonParser.parseString(json).asJsonObject
            gson.fromJson(jsonObject, TaskContextUpdate::class.java) ?: TaskContextUpdate()
        } catch (exception: JsonSyntaxException) {
            Log.d(LOG_TAG, "TASK_MEMORY_UPDATE_ERROR=${exception.message}")
            TaskContextUpdate()
        } catch (exception: IllegalStateException) {
            Log.d(LOG_TAG, "TASK_MEMORY_UPDATE_ERROR=${exception.message}")
            TaskContextUpdate()
        }
    }

    private fun String.extractJsonObject(): String {
        val cleaned = replace("```json", "")
            .replace("```", "")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) {
            throw JsonSyntaxException("No JSON object found")
        }
        return cleaned.substring(start, end + 1)
    }

    private fun String.preview(maxChars: Int): String {
        val compact = replace(Regex("\\s+"), " ").trim()
        return if (compact.length <= maxChars) compact else compact.take(maxChars) + "..."
    }

    companion object {
        private const val LOG_TAG = "TASK_MEMORY_UPDATE"
    }
}
