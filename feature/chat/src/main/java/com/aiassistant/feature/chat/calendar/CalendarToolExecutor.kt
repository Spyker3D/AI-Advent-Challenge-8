package com.aiassistant.feature.chat.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.time.*
import java.util.UUID
import javax.inject.Inject

class CalendarToolExecutor @Inject constructor(private val context: Context, private val repository: AndroidCalendarRepository, private val gson: Gson) {
    suspend fun execute(name: String, rawArguments: String): CalendarToolOutcome = when (name) {
        "list_calendar_events" -> list(rawArguments)
        "create_calendar_event" -> prepareCreate(rawArguments)
        else -> CalendarToolOutcome.Failure("LLM вызвал неизвестный инструмент: $name")
    }

    private suspend fun list(raw: String): CalendarToolOutcome {
        if (!granted(Manifest.permission.READ_CALENDAR)) return CalendarToolOutcome.Permission(Manifest.permission.READ_CALENDAR)
        return runCatching {
            val json = gson.fromJson(raw, JsonObject::class.java) ?: error("Пустые аргументы")
            val zone = ZoneId.systemDefault(); val start = CalendarDateTime.parse(json.requiredString("startDateTime"), zone); val end = CalendarDateTime.parse(json.requiredString("endDateTime"), zone)
            require(end.isAfter(start)) { "Конец диапазона должен быть позже начала" }
            val calendarId = json.get("calendarId")?.takeUnless { it.isJsonNull }?.asLong
            repository.getEvents(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli(), calendarId).getOrThrow()
        }.fold({ events -> CalendarToolOutcome.Answer(gson.toJson(mapOf("success" to true, "events" to events.map { mapOf("title" to it.title, "startDateTime" to Instant.ofEpochMilli(it.startMillis).atZone(ZoneId.systemDefault()).toString(), "endDateTime" to Instant.ofEpochMilli(it.endMillis).atZone(ZoneId.systemDefault()).toString(), "allDay" to it.isAllDay, "location" to it.location) }))) }, { CalendarToolOutcome.Failure("Не удалось прочитать календарь: ${it.message ?: "неизвестная ошибка"}") })
    }

    private suspend fun prepareCreate(raw: String): CalendarToolOutcome = runCatching {
        val json = gson.fromJson(raw, JsonObject::class.java) ?: error("Пустые аргументы")
        val title = json.requiredString("title").trim(); require(title.isNotEmpty()) { "Название события не указано" }
        val zone = ZoneId.of(json.get("timeZone")?.asString ?: ZoneId.systemDefault().id)
        val start = CalendarDateTime.parse(json.requiredString("startDateTime"), zone)
        val end = json.get("endDateTime")?.takeUnless { it.isJsonNull }?.asString?.let { CalendarDateTime.parse(it, zone) } ?: start.plusHours(1)
        require(end.isAfter(start)) { "Конец события должен быть позже начала" }
        val requestedId = json.get("calendarId")?.takeUnless { it.isJsonNull }?.asLong
        val calendar = if (granted(Manifest.permission.READ_CALENDAR)) repository.getCalendars().getOrDefault(emptyList()).let { list -> list.firstOrNull { it.id == requestedId } ?: list.firstOrNull() } else null
        PendingCalendarAction.CreateEvent(UUID.randomUUID().toString(), CalendarEventDraft(title, start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli(), zone.id, calendar?.id ?: requestedId, calendar?.name ?: "основной", json.nullableString("location"), json.nullableString("description")))
    }.fold({ CalendarToolOutcome.Pending(it) }, { CalendarToolOutcome.Failure("Некорректные параметры события: ${it.message ?: "malformed JSON"}") })

    suspend fun confirm(action: PendingCalendarAction.CreateEvent): Result<CreatedCalendarEvent> {
        if (!granted(Manifest.permission.WRITE_CALENDAR)) return Result.failure(SecurityException("WRITE_CALENDAR permission required"))
        val draft = if (action.draft.calendarId == null) { val calendar = repository.getCalendars().getOrThrow().firstOrNull() ?: error("На устройстве нет доступного календаря для записи"); action.draft.copy(calendarId = calendar.id, calendarName = calendar.name) } else action.draft
        return repository.createEvent(draft)
    }
    private fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    private fun JsonObject.requiredString(name: String) = get(name)?.takeUnless { it.isJsonNull }?.asString ?: error("Не указан $name")
    private fun JsonObject.nullableString(name: String) = get(name)?.takeUnless { it.isJsonNull }?.asString
}
