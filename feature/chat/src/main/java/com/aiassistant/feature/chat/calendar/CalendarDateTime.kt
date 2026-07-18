package com.aiassistant.feature.chat.calendar

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

object CalendarDateTime {
    fun dayRange(date: LocalDate, zone: ZoneId): Pair<Long, Long> = date.atStartOfDay(zone).toInstant().toEpochMilli() to date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    fun todayRange(clock: Clock = Clock.systemDefaultZone()) = dayRange(LocalDate.now(clock), clock.zone)
    fun tomorrowRange(clock: Clock = Clock.systemDefaultZone()) = dayRange(LocalDate.now(clock).plusDays(1), clock.zone)
    fun parse(value: String, fallbackZone: ZoneId): ZonedDateTime = runCatching { ZonedDateTime.parse(value) }.getOrElse { LocalDateTime.parse(value).atZone(fallbackZone) }
    fun formatPreview(draft: CalendarEventDraft): String { val zone=ZoneId.of(draft.timeZone); val start=Instant.ofEpochMilli(draft.startMillis).atZone(zone); val end=Instant.ofEpochMilli(draft.endMillis).atZone(zone); val date=DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")).format(start); return "Создать событие?\n\nНазвание: ${draft.title}\nДата: $date\nВремя: ${start.format(DateTimeFormatter.ofPattern("HH:mm"))}–${end.format(DateTimeFormatter.ofPattern("HH:mm"))}\nКалендарь: ${draft.calendarName}" + (draft.location?.let { "\nМесто: $it" } ?: "") }
    fun formatPreview(action: PendingCalendarAction): String = when (action) {
        is PendingCalendarAction.CreateEvent -> formatPreview(action.draft)
        is PendingCalendarAction.UpdateEvent -> "Изменить событие?\n\nБыло:\n${formatEvent(action.original)}\n\nСтанет:\n${formatDraft(action.draft)}"
        is PendingCalendarAction.DeleteEvent -> "Удалить событие?\n\n${formatEvent(action.event)}\n\nЭто действие нельзя отменить."
    }
    private fun formatEvent(event: CalendarEvent): String { val zone=ZoneId.systemDefault(); val start=Instant.ofEpochMilli(event.startMillis).atZone(zone); val end=Instant.ofEpochMilli(event.endMillis).atZone(zone); return "${event.title}\n${start.toLocalDate()}, ${start.format(DateTimeFormatter.ofPattern("HH:mm"))}–${end.format(DateTimeFormatter.ofPattern("HH:mm"))}" }
    private fun formatDraft(draft: CalendarEventDraft): String { val zone=ZoneId.of(draft.timeZone); val start=Instant.ofEpochMilli(draft.startMillis).atZone(zone); val end=Instant.ofEpochMilli(draft.endMillis).atZone(zone); return "${draft.title}\n${start.toLocalDate()}, ${start.format(DateTimeFormatter.ofPattern("HH:mm"))}–${end.format(DateTimeFormatter.ofPattern("HH:mm"))}" }
}
