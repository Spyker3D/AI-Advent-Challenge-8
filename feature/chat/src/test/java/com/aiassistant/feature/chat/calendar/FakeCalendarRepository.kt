package com.aiassistant.feature.chat.calendar

class FakeCalendarRepository : CalendarRepository {
    val createdEvents = mutableListOf<CalendarEventDraft>()
    var events: List<CalendarEvent> = emptyList()
    override suspend fun getCalendars() = Result.success(listOf(CalendarInfo(1, "Test", true)))
    override suspend fun getEvents(startMillis: Long, endMillis: Long, calendarId: Long?) = Result.success(events.sortedBy { it.startMillis })
    override suspend fun createEvent(event: CalendarEventDraft): Result<CreatedCalendarEvent> { createdEvents += event; return Result.success(CreatedCalendarEvent(42,event)) }
}
