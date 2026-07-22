package com.aiassistant.feature.chat.calendar

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class CalendarDateTimeTest {
    private val zone = ZoneId.of("Europe/Moscow")
    private val clock = Clock.fixed(Instant.parse("2026-07-18T10:30:00Z"), zone)
    @Test fun `today range uses local midnight`() { val (start,end)=CalendarDateTime.todayRange(clock); assertEquals(LocalDate.of(2026,7,18), Instant.ofEpochMilli(start).atZone(zone).toLocalDate()); assertEquals(24, Duration.ofMillis(end-start).toHours()) }
    @Test fun `tomorrow range advances one local day`() { val (start,_)=CalendarDateTime.tomorrowRange(clock); assertEquals(LocalDate.of(2026,7,19), Instant.ofEpochMilli(start).atZone(zone).toLocalDate()) }
    @Test fun `parser applies fallback timezone`() { assertEquals(zone, CalendarDateTime.parse("2026-07-19T15:00:00", zone).zone) }
    @Test fun `preview formats title and time`() { val start=ZonedDateTime.of(2026,7,19,15,0,0,0,zone); val draft=CalendarEventDraft("Day 35",start.toInstant().toEpochMilli(),start.plusHours(1).toInstant().toEpochMilli(),zone.id,1,"Основной"); assertTrue(CalendarDateTime.formatPreview(draft).contains("15:00–16:00")) }
}
