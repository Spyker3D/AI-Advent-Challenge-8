package com.aiassistant.feature.chat.calendar

data class CalendarInfo(val id: Long, val name: String, val isPrimary: Boolean)
data class CalendarEvent(val id: Long, val calendarId: Long, val title: String, val startMillis: Long, val endMillis: Long, val isAllDay: Boolean, val location: String?, val description: String?)
data class CalendarEventDraft(val title: String, val startMillis: Long, val endMillis: Long, val timeZone: String, val calendarId: Long?, val calendarName: String = "Основной", val location: String? = null, val description: String? = null)
data class CreatedCalendarEvent(val id: Long, val draft: CalendarEventDraft)

sealed interface PendingCalendarAction {
    val actionId: String
    data class CreateEvent(override val actionId: String, val draft: CalendarEventDraft) : PendingCalendarAction
}

sealed interface CalendarUiState {
    object Idle : CalendarUiState
    data class PermissionRequired(val permission: String, val permanentlyDenied: Boolean = false) : CalendarUiState
    data class PendingConfirmation(val action: PendingCalendarAction.CreateEvent) : CalendarUiState
    object Executing : CalendarUiState
    data class Success(val message: String) : CalendarUiState
    data class Error(val message: String) : CalendarUiState
}

sealed interface CalendarToolOutcome {
    data class Answer(val text: String) : CalendarToolOutcome
    data class Pending(val action: PendingCalendarAction.CreateEvent) : CalendarToolOutcome
    data class Permission(val permission: String) : CalendarToolOutcome
    data class Failure(val message: String) : CalendarToolOutcome
}
