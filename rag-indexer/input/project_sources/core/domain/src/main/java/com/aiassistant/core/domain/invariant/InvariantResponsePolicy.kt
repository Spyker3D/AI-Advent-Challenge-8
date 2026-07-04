package com.aiassistant.core.domain.invariant

object InvariantResponsePolicy {
    fun retryPrompt(fail: InvariantValidationResult.Fail): String = buildString {
        appendLine("Твой предыдущий ответ нарушил инварианты:")
        appendLine()
        fail.violations.forEach { appendLine("- $it") }
        appendLine()
        appendLine("Предыдущий ответ:")
        appendLine(fail.originalResponse)
        appendLine()
        appendLine("Перепиши ответ так, чтобы он НЕ нарушал инварианты.")
        appendLine("Если запрос пользователя конфликтует с инвариантами, явно объясни отказ и предложи допустимую альтернативу.")
        append("Отвечай на русском.")
    }

    fun safeRefusal(
        violations: List<String>,
        invariants: List<Invariant>
    ): String {
        val stack = invariants.filterIsInstance<StackInvariant>().firstOrNull()
        val architecture = invariants.filterIsInstance<ArchitectureInvariant>().firstOrNull()
        val budget = invariants.filterIsInstance<BudgetInvariant>().firstOrNull()
        val dependencies = invariants.filterIsInstance<MaxDependenciesInvariant>().firstOrNull()
        val architectureViolation = violations.any {
            it.contains("архитектур", ignoreCase = true)
        }

        return buildString {
            appendLine("Не могу предложить это решение, потому что оно нарушает инварианты:")
            appendLine()
            violations.forEach { appendLine("- $it") }
            appendLine()
            append("Допустимый вариант: ")
            if (architectureViolation && architecture != null) {
                append("использовать архитектуру ${architecture.required}")
            } else if (stack != null) {
                append("использовать ${stack.allowed.joinToString(" + ")}")
            } else {
                append("переформулировать решение в рамках разрешённых правил")
            }
            if (!architectureViolation && architecture != null) {
                append(" с архитектурой ${architecture.required}")
            }
            if (budget != null) append(" и бюджетным правилом «${budget.rule}»")
            if (dependencies != null) append(", не более ${dependencies.max} зависимостей")
            append(".")
        }
    }

    fun complianceStatement(invariants: List<Invariant>): String = buildString {
        invariants.filterIsInstance<StackInvariant>().firstOrNull()?.let {
            append("Разрешённый стек: ${it.allowed.joinToString(" + ")}. ")
        }
        invariants.filterIsInstance<ArchitectureInvariant>().firstOrNull()?.let {
            append("Архитектура: ${it.required}. ")
        }
        invariants.filterIsInstance<BudgetInvariant>().firstOrNull()?.let {
            append("Бюджет: ${it.rule}. ")
        }
        invariants.filterIsInstance<MaxDependenciesInvariant>().firstOrNull()?.let {
            append("Максимум зависимостей: ${it.max}.")
        }
    }.trim()
}
