package com.aiassistant.core.domain.memory

object ValidationReportValidator {
    private val requiredSections = listOf(
        "# Validation Report",
        "## Статус",
        "## Что выполнено хорошо",
        "## Замечания",
        "## Риски",
        "## Рекомендации",
        "## Итог"
    )

    fun isValid(validationResult: String, executionResult: String): Boolean =
        hasRequiredSections(validationResult) &&
            !looksLikeCopiedExecutionResult(validationResult, executionResult)

    fun hasRequiredSections(validationResult: String): Boolean =
        requiredSections.all(validationResult::contains)

    fun looksLikeCopiedExecutionResult(
        validationResult: String,
        executionResult: String
    ): Boolean {
        if (executionResult.isBlank()) return false

        val executionStart = executionResult
            .take(300)
            .trim()

        if (executionStart.length < 100) return false
        return validationResult.contains(executionStart)
    }
}
