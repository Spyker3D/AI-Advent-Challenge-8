package com.aiassistant.feature.chat.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AndroidCalendarRepository @Inject constructor(private val context: Context) : CalendarRepository {
    override suspend fun getCalendars() = withContext(Dispatchers.IO) { runCatching {
        val result = mutableListOf<CalendarInfo>()
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.IS_PRIMARY)
        val selection = "${CalendarContract.Calendars.VISIBLE}=1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL}>=?"
        context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, selection, arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()), null)?.use { cursor ->
            while (cursor.moveToNext()) result += CalendarInfo(cursor.getLong(0), cursor.getString(1).orEmpty().ifBlank { "Календарь" }, cursor.getInt(2) == 1)
        } ?: error("Calendar Provider недоступен")
        result.sortedWith(compareByDescending<CalendarInfo> { it.isPrimary }.thenBy { it.name })
    } }

    override suspend fun getEvents(startMillis: Long, endMillis: Long, calendarId: Long?) = withContext(Dispatchers.IO) { runCatching {
        require(endMillis > startMillis) { "Конец диапазона должен быть позже начала" }
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also { ContentUris.appendId(it, startMillis); ContentUris.appendId(it, endMillis) }.build()
        val p = arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.CALENDAR_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.EVENT_LOCATION, CalendarContract.Instances.DESCRIPTION)
        val selection = calendarId?.let { "${CalendarContract.Instances.CALENDAR_ID}=?" }
        val args = calendarId?.let { arrayOf(it.toString()) }
        val events = mutableListOf<CalendarEvent>()
        context.contentResolver.query(uri, p, selection, args, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext()) events += CalendarEvent(c.getLong(0), c.getLong(1), c.getString(2).orEmpty().ifBlank { "Без названия" }, c.getLong(3), c.getLong(4), c.getInt(5) == 1, c.getString(6), c.getString(7))
        } ?: error("Calendar Provider недоступен")
        events.sortedBy { it.startMillis }
    } }

    override suspend fun createEvent(event: CalendarEventDraft) = withContext(Dispatchers.IO) { runCatching {
        require(event.title.isNotBlank()) { "Название события не указано" }; require(event.endMillis > event.startMillis) { "Конец события должен быть позже начала" }
        val calendarId = event.calendarId ?: error("Не выбран календарь для записи")
        val values = ContentValues().apply { put(CalendarContract.Events.CALENDAR_ID, calendarId); put(CalendarContract.Events.TITLE, event.title); put(CalendarContract.Events.DTSTART, event.startMillis); put(CalendarContract.Events.DTEND, event.endMillis); put(CalendarContract.Events.EVENT_TIMEZONE, event.timeZone); event.location?.let { put(CalendarContract.Events.EVENT_LOCATION, it) }; event.description?.let { put(CalendarContract.Events.DESCRIPTION, it) } }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: error("Calendar Provider не создал событие")
        CreatedCalendarEvent(ContentUris.parseId(uri), event)
    } }
}
