package com.aiassistant.feature.chat.calendar

class FakeCalendarRepository : CalendarRepository {
    val createdEvents = mutableListOf<CalendarEventDraft>()
    val updatedEvents = mutableListOf<Pair<Long, CalendarEventDraft>>()
    val deletedEvents = mutableListOf<Long>()
    var events: List<CalendarEvent> = emptyList()
    override suspend fun getCalendars() = Result.success(listOf(CalendarInfo(1, "Test", true)))
    override suspend fun getEvents(startMillis: Long, endMillis: Long, calendarId: Long?) = Result.success(events.sortedBy { it.startMillis })
    override suspend fun createEvent(event: CalendarEventDraft): Result<CreatedCalendarEvent> { createdEvents += event; return Result.success(CreatedCalendarEvent(42,event)) }
    override suspend fun updateEvent(eventId: Long, event: CalendarEventDraft): Result<UpdatedCalendarEvent> { updatedEvents += eventId to event; return Result.success(UpdatedCalendarEvent(eventId, event)) }
    override suspend fun deleteEvent(eventId: Long, title: String): Result<DeletedCalendarEvent> { deletedEvents += eventId; return Result.success(DeletedCalendarEvent(eventId, title)) }
}
