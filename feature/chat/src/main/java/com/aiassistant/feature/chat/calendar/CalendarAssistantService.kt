package com.aiassistant.feature.chat.calendar

import android.content.Context
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

class CalendarAssistantService @Inject constructor(private val llm: LlmClient, context: Context, repository: AndroidCalendarRepository, private val gson: Gson) {
    private val executor = CalendarToolExecutor(context, repository, gson)
    private var permissionPending: Triple<String, String, String>? = null
    fun canHandle(text: String): Boolean = Regex("(?i)(календар|событи|встреч|расписан|запланирован|calendar|schedule|appointment)").containsMatchIn(text)
    suspend fun handle(userText: String): CalendarToolOutcome {
        val now = ZonedDateTime.now(); val prompt = SYSTEM + "\nCurrent local datetime: $now\nTimezone: ${now.zone.id}\nToday is ${now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}.\nUser: $userText"
        val raw = llm.sendChat(listOf(Message("calendar-system", prompt, MessageRole.SYSTEM)), 700).getOrElse { return CalendarToolOutcome.Failure("Не удалось определить календарное действие: ${it.message}") }.message
        val call = CalendarToolDefinitions.parseCall(raw, gson).getOrElse { return CalendarToolOutcome.Failure("Модель вернула некорректный calendar tool call: ${it.message}") }
        val result = executor.execute(call.first, call.second)
        if (result is CalendarToolOutcome.Permission) permissionPending = Triple(userText, call.first, call.second)
        return finish(userText, result)
    }
    suspend fun resumeAfterPermission(): CalendarToolOutcome {
        val (user, name, args) = permissionPending ?: return CalendarToolOutcome.Failure("Нет ожидающей календарной операции")
        permissionPending = null
        return finish(user, executor.execute(name, args))
    }
    private suspend fun finish(userText: String, result: CalendarToolOutcome): CalendarToolOutcome {
        if (result !is CalendarToolOutcome.Answer) return result
        val finalPrompt = "Ответь пользователю по-русски кратко, используя только фактический tool result. Не выдумывай события.\nЗапрос: $userText\nTool result: ${result.text}"
        return llm.sendChat(listOf(Message("calendar-result", finalPrompt, MessageRole.SYSTEM)), 700).fold({ CalendarToolOutcome.Answer(it.message) }, { CalendarToolOutcome.Failure("Календарь прочитан, но ответ сформировать не удалось") })
    }
    suspend fun confirm(action: PendingCalendarAction.CreateEvent) = executor.confirm(action)

    companion object { const val MAX_TOOL_ITERATIONS=5; private const val SYSTEM = """You are an AI assistant inside an Android application. For schedule reads choose list_calendar_events. For creation choose create_calendar_event. Resolve relative dates using the supplied local context. Never invent events. Writes require UI confirmation. If duration is omitted use 60 minutes. If exact time is critically missing, return {\"tool\":\"clarification\",\"arguments\":{\"message\":\"...\"}}. Return JSON only: {\"tool\":\"list_calendar_events\",\"arguments\":{\"startDateTime\":\"ISO-8601 with timezone\",\"endDateTime\":\"ISO-8601 with timezone\",\"calendarId\":null}} or {\"tool\":\"create_calendar_event\",\"arguments\":{\"title\":\"...\",\"startDateTime\":\"ISO-8601 with timezone\",\"endDateTime\":\"ISO-8601 with timezone\",\"timeZone\":\"IANA zone\",\"location\":null,\"description\":null,\"calendarId\":null}}.""" }
}
