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
}
