package com.aiassistant.core.data.mapper

internal object OllamaErrorFormatter {
    fun invalidBaseUrl(baseUrl: String): String = "Invalid Ollama Base URL: $baseUrl"

    fun emptyResponse(): String = "Empty response from local LLM"

    fun connectionError(baseUrl: String): String =
        "Не удалось подключиться к локальной LLM.\nПроверь, что Ollama запущена и доступна по $baseUrl"

    fun timeout(): String =
        "Local LLM request timed out. Check that Ollama is running and the model is responding."

    fun httpError(code: Int, detail: String, model: String): String = when (code) {
        404 -> modelNotFound(model)
        else -> "Ollama HTTP $code: $detail"
    }

    fun modelNotFound(model: String): String =
        "Модель $model не установлена.\nВыполните:\nollama pull $model"
}