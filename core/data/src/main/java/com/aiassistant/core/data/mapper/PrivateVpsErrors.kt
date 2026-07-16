package com.aiassistant.core.data.mapper

fun privateVpsHttpError(code: Int, retryAfter: String? = null): String = when (code) {
    400 -> "VPS отклонил запрос. Проверьте модель и параметры."
    401 -> "Неверный или отозванный API key VPS."
    403 -> "Demo-пользователь не имеет доступа к выбранной модели."
    404 -> "VPS API endpoint не найден. Проверьте Base URL."
    429 -> retryAfter?.let { "Превышен rate limit приватного сервиса. Повторите через $it секунд." }
        ?: "Превышен rate limit приватного сервиса. Повторите позже."
    in 500..599 -> "Приватный AI-сервис временно недоступен."
    else -> "VPS вернул ошибку HTTP $code."
}
