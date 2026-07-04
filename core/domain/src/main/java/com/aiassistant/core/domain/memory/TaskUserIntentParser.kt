package com.aiassistant.core.domain.memory

object TaskUserIntentParser {
    private val confirmationPhrases = listOf(
        "да",
        "ок",
        "окей",
        "хорошо",
        "подтверждаю",
        "согласен",
        "согласна",
        "продолжай",
        "да продолжай",
        "да подтверждаю",
        "можно продолжать",
        "переходи дальше",
        "идем дальше",
        "идём дальше",
        "go on",
        "continue",
        "confirm",
        "yes",
        "ok"
    )

    fun isConfirmation(text: String): Boolean {
        val normalized = normalize(text)
        return confirmationPhrases.any { phrase ->
            normalized == phrase ||
                Regex("(^|\\s)${Regex.escape(phrase)}(\\s|$)")
                    .containsMatchIn(normalized)
        }
    }

    fun isEditRequest(text: String): Boolean {
        val normalized = normalize(text)
        return EDIT_MARKERS.any(normalized::contains)
    }

    fun isClarifyingQuestion(text: String): Boolean {
        val normalized = normalize(text)
        return text.contains("?") || QUESTION_MARKERS.any(normalized::contains)
    }

    fun isPause(text: String): Boolean =
        containsAny(text, "пауза", "останови", "стоп", "pause", "stop")

    fun isResume(text: String): Boolean =
        containsAny(text, "продолжи", "resume", "continue")

    fun isTaskStatus(text: String): Boolean =
        containsAny(text, "статус задачи", "task status", "статус")

    fun parseRequestedStage(text: String): TaskStage? {
        val normalized = normalize(text)
        val transitionRequested = listOf(
            "перейди", "переход", "пропусти", "сразу", "переходи",
            "go to", "skip"
        ).any(normalized::contains)
        if (!transitionRequested) return null

        return when {
            "planning" in normalized -> TaskStage.PLANNING
            "execution" in normalized -> TaskStage.EXECUTION
            "validation" in normalized -> TaskStage.VALIDATION
            Regex("""\bdone\b""").containsMatchIn(normalized) -> TaskStage.DONE
            else -> null
        }
    }

    private fun containsAny(text: String, vararg phrases: String): Boolean {
        val normalized = normalize(text)
        return phrases.any(normalized::contains)
    }

    fun normalize(text: String): String =
        text.lowercase()
            .trim()
            .replace(Regex("[!?.。,，:;]+"), "")
            .replace(Regex("""\s+"""), " ")

    private val EDIT_MARKERS = listOf(
        "добавь", "дополни", "измени", "исправь", "перепиши", "убери",
        "удали", "замени", "учти", "внеси", "расширь", "сократи",
        "переформулируй", "добавить", "изменить", "исправить", "убрать",
        "удалить", "заменить", "add", "remove", "delete", "update",
        "change", "rewrite", "fix"
    )

    private val QUESTION_MARKERS = listOf(
        "почему", "зачем", "как", "что значит", "что такое", "объясни",
        "поясни", "можешь пояснить", "можешь объяснить", "почему ты",
        "зачем ты", "why", "how", "what", "explain"
    )
}
