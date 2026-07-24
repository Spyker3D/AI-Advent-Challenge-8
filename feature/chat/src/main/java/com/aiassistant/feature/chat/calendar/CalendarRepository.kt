package com.aiassistant.feature.chat.calendar

interface CalendarRepository {
    suspend fun getCalendars(): Result<List<CalendarInfo>>
    suspend fun getEvents(startMillis: Long, endMillis: Long, calendarId: Long? = null): Result<List<CalendarEvent>>
    suspend fun createEvent(event: CalendarEventDraft): Result<CreatedCalendarEvent>
    suspend fun updateEvent(eventId: Long, event: CalendarEventDraft): Result<UpdatedCalendarEvent>
    suspend fun deleteEvent(eventId: Long, title: String): Result<DeletedCalendarEvent>
}
