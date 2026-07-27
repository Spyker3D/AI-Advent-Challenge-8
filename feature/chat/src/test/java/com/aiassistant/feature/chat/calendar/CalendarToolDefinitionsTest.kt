package com.aiassistant.feature.chat.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarToolDefinitionsTest {

    @Test
    fun `parses list tool arguments`() {
        val result = CalendarToolDefinitions.parseCall(
            """{"tool":"list_calendar_events","arguments":{"startDateTime":"2026-07-18T00:00:00+03:00"}}"""
        ).getOrThrow()

        assertEquals("list_calendar_events", result.first)
        assertTrue(result.second.contains("startDateTime"))
    }

    @Test
    fun `empty and malformed JSON are rejected`() {
        assertTrue(CalendarToolDefinitions.parseCall("").isFailure)
        assertTrue(CalendarToolDefinitions.parseCall("   ").isFailure)
        assertTrue(CalendarToolDefinitions.parseCall("not-json").isFailure)
    }

    @Test
    fun `missing tool is rejected`() {
        assertTrue(CalendarToolDefinitions.parseCall("""{"arguments":{}}""").isFailure)
    }

    @Test
    fun `unknown tool is rejected`() {
        assertTrue(
            CalendarToolDefinitions.parseCall(
                """{"tool":"delete_everything","arguments":{}}"""
            ).isFailure
        )
    }

    @Test
    fun `missing arguments are rejected`() {
        assertTrue(
            CalendarToolDefinitions.parseCall(
                """{"tool":"list_calendar_events"}"""
            ).isFailure
        )
    }

    @Test
    fun `update and delete tools are supported`() {
        assertTrue("update_calendar_event" in CalendarToolDefinitions.supported)
        assertTrue("delete_calendar_event" in CalendarToolDefinitions.supported)
    }
}