package com.aiassistant.core.domain.invariant

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class InvariantValidatorTest {
    private val validator = InvariantValidator()
    private val invariants = listOf(
        StackInvariant(
            allowed = setOf("Kotlin", "Ktor"),
            banned = setOf("Java", "Spring Boot", "RxJava")
        ),
        ArchitectureInvariant(
            required = "MVVM",
            banned = setOf("MVC", "MVP")
        ),
        BudgetInvariant(rule = "Only free APIs"),
        MaxDependenciesInvariant(max = 2)
    )

    @Test
    fun `passes response that satisfies all invariants`() {
        val result = validator.validateResponse(
            "Решение: Kotlin + Ktor, архитектура MVVM, используются бесплатные API.",
            invariants
        )

        assertTrue(result is InvariantValidationResult.Pass)
    }

    @Test
    fun `banned stack fails even when allowed stack is present`() {
        val result = validator.validateResponse(
            "Можно использовать Kotlin + Spring Boot в архитектуре MVVM.",
            invariants
        )

        assertTrue(result is InvariantValidationResult.Fail)
    }

    @Test
    fun `stack invariant does not require allowed stack mention in every response`() {
        val result = validator.validateResponse(
            "Нельзя использовать MVC, требуется MVVM.",
            listOf(
                StackInvariant(
                    allowed = setOf("Kotlin", "Ktor"),
                    banned = setOf("Java", "Spring Boot", "RxJava")
                )
            )
        )

        assertTrue(result is InvariantValidationResult.Pass)
    }

    @Test
    fun `architecture invariant fails on banned architecture`() {
        val result = validator.validateResponse(
            "Можно использовать MVC для простоты.",
            listOf(
                ArchitectureInvariant(
                    required = "MVVM",
                    banned = setOf("MVC", "MVP")
                )
            )
        )

        assertTrue(result is InvariantValidationResult.Fail)
    }

    @Test
    fun `mvc request should produce architecture violation not stack violation`() {
        val result = validator.validateResponse(
            "Можно использовать MVC для простоты.",
            listOf(
                StackInvariant(
                    allowed = setOf("Kotlin", "Ktor"),
                    banned = setOf("Java", "Spring Boot", "RxJava")
                ),
                ArchitectureInvariant(
                    required = "MVVM",
                    banned = setOf("MVC", "MVP")
                )
            )
        )

        assertTrue(result is InvariantValidationResult.Fail)
        val fail = result as InvariantValidationResult.Fail
        assertTrue(fail.violations.any { it.contains("архитектур", ignoreCase = true) })
        assertTrue(fail.violations.none { it.contains("стека", ignoreCase = true) })
    }

    @Test
    fun `architecture refusal prioritizes required architecture over stack`() {
        val architecture = ArchitectureInvariant(
            required = "MVVM",
            banned = setOf("MVC", "MVP")
        )
        val refusal = InvariantResponsePolicy.safeRefusal(
            violations = listOf(architecture.violationMessage()),
            invariants = invariants
        )

        assertTrue(refusal.contains("использовать архитектуру MVVM", ignoreCase = true))
        assertTrue(!refusal.contains("использовать Kotlin + Ktor", ignoreCase = true))
    }

    @Test
    fun `stack invariant passes when banned technologies are mentioned as forbidden`() {
        val invariant = StackInvariant(
            allowed = setOf("Kotlin", "Ktor"),
            banned = setOf("Java", "Spring Boot", "RxJava")
        )
        val response = """
            Не используем Java, Spring Boot и RxJava.
            Допустимый вариант: Kotlin + Ktor.
        """.trimIndent()

        assertTrue(invariant.check(response))
    }

    @Test
    fun `stack invariant fails when banned technology is proposed`() {
        val invariant = StackInvariant(
            allowed = setOf("Kotlin", "Ktor"),
            banned = setOf("Java", "Spring Boot", "RxJava")
        )

        assertFalse(invariant.check("Предлагаю использовать Spring Boot для REST API."))
    }

    @Test
    fun `stack invariant passes when allowed stack is presented as alternative`() {
        val invariant = StackInvariant(
            allowed = setOf("Kotlin", "Ktor"),
            banned = setOf("Java", "Spring Boot", "RxJava")
        )

        assertTrue(invariant.check("Ktor — альтернатива Spring Boot."))
    }

    @Test
    fun `stack invariant passes when banned stack is explicitly not applied`() {
        val invariant = StackInvariant(
            allowed = setOf("Kotlin", "Ktor"),
            banned = setOf("Java", "Spring Boot", "RxJava")
        )

        assertTrue(invariant.check("Не применяем Java, Spring Boot и RxJava."))
    }

    @Test
    fun `stack invariant passes neutral response without stack mention`() {
        val invariant = StackInvariant(
            allowed = setOf("Kotlin", "Ktor"),
            banned = setOf("Java", "Spring Boot", "RxJava")
        )

        assertTrue(
            invariant.check(
                "Нужно спроектировать REST API с эндпоинтами задач, пользователей и статусов."
            )
        )
    }

    @Test
    fun `architecture invariant passes when MVC is mentioned as forbidden`() {
        val invariant = ArchitectureInvariant(
            required = "MVVM",
            banned = setOf("MVC", "MVP")
        )

        assertTrue(invariant.check("MVC использовать нельзя, требуется MVVM."))
    }

    @Test
    fun `architecture invariant fails when MVC is proposed`() {
        val invariant = ArchitectureInvariant(
            required = "MVVM",
            banned = setOf("MVC", "MVP")
        )

        assertFalse(invariant.check("Предлагаю использовать MVC для простоты."))
    }

    @Test
    fun `budget invariant passes when paid API is mentioned as forbidden`() {
        val invariant = BudgetInvariant(rule = "Only free APIs")

        assertTrue(invariant.check("Платные API использовать нельзя."))
    }

    @Test
    fun `ordinary REST API planning response passes default invariants`() {
        val response = """
            # Итоговый план
            Реализовать REST API для задач на Kotlin и Ktor.
            Использовать архитектуру MVVM и только бесплатные API.
            Добавить эндпоинты задач, пользователей и статусов, затем покрыть их тестами.
        """.trimIndent()

        assertTrue(
            validator.validateResponse(response, defaultInvariants()) is
                InvariantValidationResult.Pass
        )
    }

    @Test
    fun `safe refusal passes validation despite mentioning banned stack`() {
        val defaults = defaultInvariants()
        val stackViolation = defaults
            .filterIsInstance<StackInvariant>()
            .first()
            .violationMessage()
        val refusal = InvariantResponsePolicy.safeRefusal(
            violations = listOf(stackViolation),
            invariants = defaults
        )

        assertTrue(
            validator.validateResponse(refusal, defaults) is InvariantValidationResult.Pass
        )
    }

    @Test
    fun `too many dependency declarations fail`() {
        val result = validator.validateResponse(
            """
            Kotlin + Ktor, MVVM.
            implementation("one")
            api("two")
            ksp("three")
            """.trimIndent(),
            invariants
        )

        assertTrue(result is InvariantValidationResult.Fail)
    }
}
