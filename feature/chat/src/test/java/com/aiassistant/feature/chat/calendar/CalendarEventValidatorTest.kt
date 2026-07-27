package com.aiassistant.feature.chat.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalendarEventValidatorTest {

    @Test
    fun `rejects blank title`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            CalendarEventValidator.validate(draft(title = "   "))
        }

        assertEquals("Название события не указано", error.message)
    }

    @Test
    fun `rejects end before or equal to start`() {
        val before = assertThrows(IllegalArgumentException::class.java) {
            CalendarEventValidator.validate(draft(start = 20, end = 10))
        }
        val equal = assertThrows(IllegalArgumentException::class.java) {
            CalendarEventValidator.validate(draft(start = 20, end = 20))
        }

        assertEquals("Конец события должен быть позже начала", before.message)
        assertEquals("Конец события должен быть позже начала", equal.message)
    }

    @Test
    fun `accepts valid draft`() {
        CalendarEventValidator.validate(draft(title = "Meeting", start = 10, end = 20))
    }

    private fun draft(
        title: String = "Meeting",
        start: Long = 10,
        end: Long = 20
    ) = CalendarEventDraft(title, start, end, "UTC", calendarId = 1)
}