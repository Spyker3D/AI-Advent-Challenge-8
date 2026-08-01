package com.aiassistant.feature.chat.calendar

object CalendarEventValidator {
    fun validate(draft: CalendarEventDraft) {
        require(draft.title.isNotBlank()) { "Название события не указано" }
        require(draft.endMillis > draft.startMillis) {
            "Конец события должен быть позже начала"
        }
    }
}