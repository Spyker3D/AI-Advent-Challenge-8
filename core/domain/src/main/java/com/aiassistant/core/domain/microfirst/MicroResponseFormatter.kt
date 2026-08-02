package com.aiassistant.core.domain.microfirst

import com.aiassistant.core.domain.inference.IncidentCategory

object MicroResponseFormatter {
    fun format(category: IncidentCategory): String? = when (category) {
        IncidentCategory.NETWORK_UNAVAILABLE -> format("Нет подключения к сети", "Проверьте интернет-соединение.", "Проверить подключение")
        IncidentCategory.OPENAI_RATE_LIMIT -> format("Превышен лимит запросов", "Подождите немного и повторите запрос.", "Повторить позже")
        IncidentCategory.OPENAI_TIMEOUT -> format("Время ожидания истекло", "Ответ не был получен вовремя.", "Повторить запрос")
        IncidentCategory.EMPTY_AI_RESPONSE -> format("Получен пустой ответ", "Модель не вернула содержимое.", "Повторить запрос")
        IncidentCategory.LOCAL_HISTORY_UNAVAILABLE -> format("История чатов недоступна", "Не удалось загрузить локальную историю.", "Перезагрузить историю")
        IncidentCategory.AMBIGUOUS -> null
    }

    fun format(title: String, message: String, action: String): String =
        "$title\n\n$message\n\n$action"
}
