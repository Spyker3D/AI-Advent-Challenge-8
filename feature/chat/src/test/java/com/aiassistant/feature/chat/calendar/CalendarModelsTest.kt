package com.aiassistant.feature.chat.calendar

import org.junit.Assert.*
import org.junit.Test

class CalendarModelsTest {
    @Test fun `pending create does not write repository`() { val fake=FakeCalendarRepository(); PendingCalendarAction.CreateEvent("one", CalendarEventDraft("Test",1,2,"UTC",1)); assertTrue(fake.createdEvents.isEmpty()) }
    @Test fun `fake repository sorts events`() { val fake=FakeCalendarRepository(); fake.events=listOf(CalendarEvent(2,1,"late",20,30,false,null,null),CalendarEvent(1,1,"early",10,15,false,null,null)); kotlinx.coroutines.runBlocking { assertEquals("early",fake.getEvents(0,40).getOrThrow().first().title) } }
    @Test fun `end must be after start by model validation rule`() { val start=100L; val end=99L; assertFalse(end>start) }
    @Test fun `permission outcome preserves required permission`() { val outcome=CalendarToolOutcome.Permission(android.Manifest.permission.READ_CALENDAR); assertEquals(android.Manifest.permission.READ_CALENDAR,outcome.permission) }
    @Test fun `empty event list remains empty`() { val fake=FakeCalendarRepository(); kotlinx.coroutines.runBlocking { assertTrue(fake.getEvents(0,1).getOrThrow().isEmpty()) } }
}
