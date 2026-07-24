package com.aiassistant.feature.chat.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.time.*
import java.util.UUID
import javax.inject.Inject

class CalendarToolExecutor @Inject constructor(private val context: Context, private val repository: AndroidCalendarRepository, private val gson: Gson) {
    suspend fun execute(name: String, rawArguments: String): CalendarToolOutcome = when (name) {
        "list_calendar_events" -> list(rawArguments)
        "find_calendar_events" -> find(rawArguments)
        "create_calendar_event" -> prepareCreate(rawArguments)
        "update_calendar_event" -> prepareUpdate(rawArguments)
        "delete_calendar_event" -> prepareDelete(rawArguments)
        "clarification" -> runCatching { gson.fromJson(rawArguments, JsonObject::class.java).requiredString("message") }.fold({ CalendarToolOutcome.Answer(gson.toJson(mapOf("success" to true, "clarification" to it))) }, { CalendarToolOutcome.Failure("Не удалось прочитать уточняющий вопрос") })
        else -> CalendarToolOutcome.Failure("LLM вызвал неизвестный инструмент: $name")
    }

    private suspend fun find(raw: String): CalendarToolOutcome {
        if (!granted(Manifest.permission.READ_CALENDAR)) return CalendarToolOutcome.Permission(Manifest.permission.READ_CALENDAR)
        return runCatching { findCandidates(gson.fromJson(raw, JsonObject::class.java) ?: error("Пустые аргументы")) }
            .fold({ events -> CalendarToolOutcome.Answer(eventsResult(events)) }, { CalendarToolOutcome.Failure("Не удалось найти события: ${it.message}") })
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

    private suspend fun prepareCreate(raw: String): CalendarToolOutcome {
        // CalendarContract.Calendars requires READ_CALENDAR on real devices. Resolve the
        // writable target before showing preview so confirmation never has a null calendarId.
        if (!granted(Manifest.permission.READ_CALENDAR)) return CalendarToolOutcome.Permission(Manifest.permission.READ_CALENDAR)
        return runCatching {
        val json = gson.fromJson(raw, JsonObject::class.java) ?: error("Пустые аргументы")
        val title = json.requiredString("title").trim(); require(title.isNotEmpty()) { "Название события не указано" }
        val zone = ZoneId.of(json.get("timeZone")?.asString ?: ZoneId.systemDefault().id)
        val start = CalendarDateTime.parse(json.requiredString("startDateTime"), zone)
        val end = json.get("endDateTime")?.takeUnless { it.isJsonNull }?.asString?.let { CalendarDateTime.parse(it, zone) } ?: start.plusHours(1)
        require(end.isAfter(start)) { "Конец события должен быть позже начала" }
        val requestedId = json.get("calendarId")?.takeUnless { it.isJsonNull }?.asLong
        val calendar = repository.getCalendars().getOrThrow().let { list -> list.firstOrNull { it.id == requestedId } ?: list.firstOrNull() } ?: error("На устройстве нет доступного календаря для записи")
        PendingCalendarAction.CreateEvent(UUID.randomUUID().toString(), CalendarEventDraft(title, start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli(), zone.id, calendar.id, calendar.name, json.nullableString("location"), json.nullableString("description")))
    }.fold({ CalendarToolOutcome.Pending(it) }, { CalendarToolOutcome.Failure("Некорректные параметры события: ${it.message ?: "malformed JSON"}") })
    }

    private suspend fun prepareUpdate(raw: String): CalendarToolOutcome {
        if (!granted(Manifest.permission.READ_CALENDAR)) return CalendarToolOutcome.Permission(Manifest.permission.READ_CALENDAR)
        return runCatching {
            val json = gson.fromJson(raw, JsonObject::class.java) ?: error("Пустые аргументы")
            val matches = findCandidates(json)
            if (matches.size != 1) return CalendarToolOutcome.Answer(eventsResult(matches, if (matches.isEmpty()) "EVENT_NOT_FOUND" else "AMBIGUOUS_EVENT"))
            val original = matches.single(); require(!original.isRecurring) { "Повторяющиеся события пока нельзя изменять" }
            val zone = ZoneId.of(json.get("timeZone")?.asString ?: ZoneId.systemDefault().id)
            val start = json.get("newStartDateTime")?.takeUnless { it.isJsonNull }?.asString?.let { CalendarDateTime.parse(it, zone) } ?: Instant.ofEpochMilli(original.startMillis).atZone(zone)
            val duration = Duration.ofMillis(original.endMillis - original.startMillis)
            val end = json.get("newEndDateTime")?.takeUnless { it.isJsonNull }?.asString?.let { CalendarDateTime.parse(it, zone) } ?: start.plus(duration)
            require(end.isAfter(start)) { "Конец события должен быть позже начала" }
            val draft = CalendarEventDraft(json.nullableString("newTitle") ?: original.title, start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli(), zone.id, original.calendarId, location = json.nullableString("newLocation") ?: original.location, description = original.description)
            PendingCalendarAction.UpdateEvent(UUID.randomUUID().toString(), original, draft)
        }.fold({ CalendarToolOutcome.Pending(it) }, { CalendarToolOutcome.Failure("Не удалось подготовить изменение: ${it.message}") })
    }

    private suspend fun prepareDelete(raw: String): CalendarToolOutcome {
        if (!granted(Manifest.permission.READ_CALENDAR)) return CalendarToolOutcome.Permission(Manifest.permission.READ_CALENDAR)
        return runCatching {
            val json = gson.fromJson(raw, JsonObject::class.java) ?: error("Пустые аргументы")
            val matches = findCandidates(json)
            if (matches.size != 1) return CalendarToolOutcome.Answer(eventsResult(matches, if (matches.isEmpty()) "EVENT_NOT_FOUND" else "AMBIGUOUS_EVENT"))
            val event = matches.single(); require(!event.isRecurring) { "Повторяющиеся события пока нельзя удалять" }
            PendingCalendarAction.DeleteEvent(UUID.randomUUID().toString(), event)
        }.fold({ CalendarToolOutcome.Pending(it) }, { CalendarToolOutcome.Failure("Не удалось подготовить удаление: ${it.message}") })
    }

    private suspend fun findCandidates(json: JsonObject): List<CalendarEvent> {
        val zone = ZoneId.systemDefault(); val start = CalendarDateTime.parse(json.requiredString("searchStartDateTime"), zone); val end = CalendarDateTime.parse(json.requiredString("searchEndDateTime"), zone)
        require(end.isAfter(start)) { "Конец диапазона поиска должен быть позже начала" }
        val query = json.requiredString("query").trim()
        return repository.getEvents(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli()).getOrThrow().filter { it.title.contains(query, ignoreCase = true) }
    }

    private fun eventsResult(events: List<CalendarEvent>, code: String? = null): String = gson.toJson(mapOf("success" to true, "resultCode" to code, "events" to events.map { mapOf("title" to it.title, "startDateTime" to Instant.ofEpochMilli(it.startMillis).atZone(ZoneId.systemDefault()).toString(), "endDateTime" to Instant.ofEpochMilli(it.endMillis).atZone(ZoneId.systemDefault()).toString(), "allDay" to it.isAllDay, "recurring" to it.isRecurring) }))

    suspend fun confirm(action: PendingCalendarAction): Result<String> {
        if (!granted(Manifest.permission.WRITE_CALENDAR)) return Result.failure(SecurityException("WRITE_CALENDAR permission required"))
        return try {
            withTimeout(CALENDAR_WRITE_TIMEOUT_MS) {
                when (action) {
                    is PendingCalendarAction.CreateEvent -> {
                        if (action.draft.calendarId == null) Result.failure(IllegalStateException("Не выбран календарь для записи"))
                        else repository.createEvent(action.draft).map { "Событие «${it.draft.title}» добавлено в календарь." }
                    }
                    is PendingCalendarAction.UpdateEvent -> repository.updateEvent(action.original.id, action.draft).map { "Событие «${it.draft.title}» изменено." }
                    is PendingCalendarAction.DeleteEvent -> repository.deleteEvent(action.event.id, action.event.title).map { "Событие «${it.title}» удалено из календаря." }
                }
            }
        } catch (_: TimeoutCancellationException) {
            Result.failure(CalendarWriteTimeoutException())
        }
    }
    private fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    private fun JsonObject.requiredString(name: String) = get(name)?.takeUnless { it.isJsonNull }?.asString ?: error("Не указан $name")
    private fun JsonObject.nullableString(name: String) = get(name)?.takeUnless { it.isJsonNull }?.asString

    companion object {
        const val CALENDAR_WRITE_TIMEOUT_MS = 15_000L
    }
}

class CalendarWriteTimeoutException : Exception("Календарь не ответил за 15 секунд. Проверьте событие и повторите операцию.")
