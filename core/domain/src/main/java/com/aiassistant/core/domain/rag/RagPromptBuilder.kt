package com.aiassistant.core.domain.rag

import javax.inject.Inject

class RagPromptBuilder @Inject constructor() {
    fun build(
        question: String,
        results: List<RagSearchResult>
    ): String {
        val context = results.mapIndexed { index, result ->
            val chunk = result.chunk
            """
            |[${index + 1}] Source: ${chunk.source}
            |Section: ${chunk.section ?: "N/A"}
            |Text:
            |${chunk.text}
            |""".trimMargin()
        }.joinToString(separator = "\n\n")

        return """
            |Ты Android AI Assistant.
            |Ответь на вопрос пользователя, используя только контекст ниже.
            |Если в контексте нет ответа, честно скажи, что в базе знаний нет достаточной информации.
            |
            |Контекст:
            |$context
            |
            |Вопрос:
            |$question
            |
            |В конце ответа добавь раздел "Использованные источники".
            |В этом разделе укажи реальные названия источников и секций из контекста, а не только номера.
            |Формат:
            |- input/docs/example.md / Section name
        """.trimMargin()
    }
}
