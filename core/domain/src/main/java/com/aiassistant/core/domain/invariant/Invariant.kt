package com.aiassistant.core.domain.invariant

sealed class Invariant {
    abstract val id: String
    abstract val description: String

    abstract fun check(response: String): Boolean

    open fun violationMessage(): String = "Нарушен инвариант: $description"
}

internal object InvariantTextMatcher {
    fun containsBannedUsage(
        response: String,
        banned: Set<String>
    ): Boolean {
        val normalizedResponse = response.lowercase()

        return banned.any { bannedTerm ->
            val normalizedTerm = bannedTerm.lowercase()
            normalizedResponse.indicesOf(normalizedTerm).any { termStart ->
                val windowStart = (termStart - CONTEXT_WINDOW_CHARS).coerceAtLeast(0)
                val windowEnd = (termStart + normalizedTerm.length + CONTEXT_WINDOW_CHARS)
                    .coerceAtMost(normalizedResponse.length)
                val localWindow = normalizedResponse.substring(windowStart, windowEnd)
                !isNegativeOrExplanatoryContext(localWindow)
            }
        }
    }

    private fun isNegativeOrExplanatoryContext(line: String): Boolean =
        NEGATIVE_CONTEXT_MARKERS.any(line::contains)

    private fun String.indicesOf(term: String): Sequence<Int> = sequence {
        var start = indexOf(term)
        while (start >= 0) {
            yield(start)
            start = indexOf(term, start + term.length)
        }
    }

    private val NEGATIVE_CONTEXT_MARKERS = listOf(
        "не использовать",
        "не используем",
        "нельзя использовать",
        "использовать нельзя",
        "нельзя предложить",
        "не могу предложить",
        "не применять",
        "не применяем",
        "не предлагать",
        "не предлагаем",
        "исключаем",
        "исключается",
        "отказаться от",
        "вместо",
        "альтернатива",
        "замена",
        "не подходит",
        "нарушает инварианты",
        "запрещено",
        "запрещён",
        "запрещена",
        "запрещены",
        "не входит",
        "не входят",
        "не разрешено",
        "не разрешены",
        "исключить",
        "без ",
        "avoid",
        "do not use",
        "don't use",
        "must not use",
        "banned",
        "forbidden",
        "not allowed",
        "not permitted",
        "not part of allowed stack",
        "alternative to",
        "instead of",
        "replace"
    )

    private const val CONTEXT_WINDOW_CHARS = 96
}

data class StackInvariant(
    override val id: String = "stack",
    val allowed: Set<String>,
    val banned: Set<String> = emptySet()
) : Invariant() {
    override val description: String = buildString {
        append("Stack invariant. ")
        append("Allowed: ${allowed.joinToString()}. ")
        if (banned.isNotEmpty()) append("Banned: ${banned.joinToString()}.")
    }

    override fun check(response: String): Boolean =
        !InvariantTextMatcher.containsBannedUsage(response, banned)

    override fun violationMessage(): String = buildString {
        append("Нарушен инвариант стека. ")
        append("Разрешено: ${allowed.joinToString()}. ")
        if (banned.isNotEmpty()) append("Запрещено: ${banned.joinToString()}.")
    }
}

data class ArchitectureInvariant(
    override val id: String = "architecture",
    val required: String,
    val banned: Set<String> = emptySet()
) : Invariant() {
    override val description: String = buildString {
        append("Architecture invariant. ")
        append("Required: $required. ")
        if (banned.isNotEmpty()) append("Banned: ${banned.joinToString()}.")
    }

    override fun check(response: String): Boolean =
        !InvariantTextMatcher.containsBannedUsage(response, banned)

    override fun violationMessage(): String = buildString {
        append("Нарушен архитектурный инвариант. ")
        append("Требуемая архитектура: $required. ")
        if (banned.isNotEmpty()) append("Запрещено: ${banned.joinToString()}.")
    }
}

data class BudgetInvariant(
    override val id: String = "budget",
    val rule: String
) : Invariant() {
    override val description: String = "Budget rule: $rule"

    override fun check(response: String): Boolean {
        if (!rule.contains("free", ignoreCase = true) &&
            !rule.contains("бесплат", ignoreCase = true)
        ) {
            return true
        }
        return !InvariantTextMatcher.containsBannedUsage(
            response = response,
            banned = PAID_MARKERS
        )
    }

    override fun violationMessage(): String = "Нарушен бюджетный инвариант: $rule."

    private companion object {
        val PAID_MARKERS = setOf(
            "paid",
            "subscription",
            "license fee",
            "платный",
            "подписка",
            "коммерческая лицензия"
        )
    }
}

data class MaxDependenciesInvariant(
    override val id: String = "max_dependencies",
    val max: Int
) : Invariant() {
    override val description: String = "Max dependencies: <= $max"

    override fun check(response: String): Boolean {
        val markers = listOf("implementation(", "api(", "kapt(", "ksp(")
        return response.lines().count { line -> markers.any(line::contains) } <= max
    }

    override fun violationMessage(): String =
        "Нарушен инвариант зависимостей. Максимум зависимостей: $max."
}

fun defaultInvariants(): List<Invariant> = listOf(
    StackInvariant(
        allowed = linkedSetOf("Kotlin", "Ktor"),
        banned = linkedSetOf("Java", "Spring Boot", "RxJava")
    ),
    ArchitectureInvariant(
        required = "MVVM",
        banned = linkedSetOf("MVC", "MVP")
    ),
    BudgetInvariant(rule = "Only free APIs"),
    MaxDependenciesInvariant(max = 5)
)
