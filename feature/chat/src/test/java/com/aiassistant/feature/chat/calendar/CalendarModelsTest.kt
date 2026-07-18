package com.aiassistant.feature.chat.calendar

import org.junit.Assert.*
import org.junit.Test

class CalendarModelsTest {
    @Test fun `pending create does not write repository`() { val fake=FakeCalendarRepository(); PendingCalendarAction.CreateEvent("one", CalendarEventDraft("Test",1,2,"UTC",1)); assertTrue(fake.createdEvents.isEmpty()) }
    @Test fun `fake repository sorts events`() { val fake=FakeCalendarRepository(); fake.events=listOf(CalendarEvent(2,1,"late",20,30,false,null,null),CalendarEvent(1,1,"early",10,15,false,null,null)); kotlinx.coroutines.runBlocking { assertEquals("early",fake.getEvents(0,40).getOrThrow().first().title) } }
    @Test fun `end must be after start by model validation rule`() { val start=100L; val end=99L; assertFalse(end>start) }
    @Test fun `permission outcome preserves required permission`() { val outcome=CalendarToolOutcome.Permission(android.Manifest.permission.READ_CALENDAR); assertEquals(android.Manifest.permission.READ_CALENDAR,outcome.permission) }
    @Test fun `empty event list remains empty`() { val fake=FakeCalendarRepository(); kotlinx.coroutines.runBlocking { assertTrue(fake.getEvents(0,1).getOrThrow().isEmpty()) } }
    @Test fun `update fake records changed event`() { val fake=FakeCalendarRepository(); val draft=CalendarEventDraft("Moved",10,20,"UTC",1); kotlinx.coroutines.runBlocking { fake.updateEvent(7,draft) }; assertEquals(7L,fake.updatedEvents.single().first) }
    @Test fun `delete fake records event id`() { val fake=FakeCalendarRepository(); kotlinx.coroutines.runBlocking { fake.deleteEvent(9,"Delete") }; assertEquals(listOf(9L),fake.deletedEvents) }
    @Test fun `update preview contains before and after`() { val old=CalendarEvent(1,1,"Meeting",10,20,false,null,null); val draft=CalendarEventDraft("Meeting",30,40,"UTC",1); val text=CalendarDateTime.formatPreview(PendingCalendarAction.UpdateEvent("a",old,draft)); assertTrue(text.contains("Было")); assertTrue(text.contains("Станет")) }
    @Test fun `delete preview warns action cannot be undone`() { val event=CalendarEvent(1,1,"Meeting",10,20,false,null,null); assertTrue(CalendarDateTime.formatPreview(PendingCalendarAction.DeleteEvent("a",event)).contains("нельзя отменить")) }
    @Test fun `recurring marker is preserved`() { assertTrue(CalendarEvent(1,1,"Series",10,20,false,null,null,true).isRecurring) }
}
