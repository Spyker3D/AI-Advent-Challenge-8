package com.aiassistant.core.domain.memory

import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.invariant.InvariantValidator
import com.aiassistant.core.domain.repository.InvariantRepository
import javax.inject.Inject

class PlanningSwarmAgentFactory @Inject constructor(
    private val llmClient: LlmClient,
    private val invariantRepository: InvariantRepository,
    private val invariantValidator: InvariantValidator,
    private val promptBuilder: PromptBuilder
) {
    fun create(role: PlanningSwarmRole): PlanningSwarmAgent {
        require(role != PlanningSwarmRole.SUPERVISOR)
        return PlanningSwarmAgent(
            role = role,
            systemPrompt = promptFor(role),
            llmClient = llmClient,
            invariantRepository = invariantRepository,
            invariantValidator = invariantValidator,
            promptBuilder = promptBuilder
        )
    }

    private fun promptFor(role: PlanningSwarmRole): String = when (role) {
        PlanningSwarmRole.REQUIREMENTS -> REQUIREMENTS_PROMPT
        PlanningSwarmRole.ARCHITECTURE -> ARCHITECTURE_PROMPT
        PlanningSwarmRole.RISKS -> RISKS_PROMPT
        PlanningSwarmRole.TESTING -> TESTING_PROMPT
        PlanningSwarmRole.IMPLEMENTATION -> IMPLEMENTATION_PROMPT
        PlanningSwarmRole.SUPERVISOR -> error("Supervisor has a dedicated agent")
    }

    private companion object {
        const val REQUIREMENTS_PROMPT = """Ты RequirementsPlanningAgent.

Ты анализируешь задачу только с точки зрения требований.

Твоя задача:
- выделить функциональные требования;
- выделить нефункциональные требования;
- определить ограничения;
- указать, какие вопросы нужно уточнить.

Не пиши финальный план.
Не переходи к EXECUTION.
Отвечай на русском языке."""

        const val ARCHITECTURE_PROMPT = """Ты ArchitecturePlanningAgent.

Ты анализируешь задачу только с точки зрения архитектуры.

Твоя задача:
- предложить архитектурный подход;
- проверить соответствие архитектурным инвариантам;
- указать основные модули/слои;
- указать архитектурные риски.

Не пиши финальный план.
Не переходи к EXECUTION.
Отвечай на русском языке."""

        const val RISKS_PROMPT = """Ты RiskPlanningAgent.

Ты анализируешь задачу только с точки зрения рисков.

Твоя задача:
- найти технические риски;
- найти продуктовые риски;
- найти edge cases;
- указать, что может сломаться.

Не пиши финальный план.
Не переходи к EXECUTION.
Отвечай на русском языке."""

        const val TESTING_PROMPT = """Ты TestingPlanningAgent.

Ты анализируешь задачу только с точки зрения тестирования.

Твоя задача:
- предложить unit tests;
- предложить integration tests;
- предложить UI tests, если уместно;
- указать критерии проверки.

Не пиши финальный план.
Не переходи к EXECUTION.
Отвечай на русском языке."""

        const val IMPLEMENTATION_PROMPT = """Ты ImplementationPlanningAgent.

Ты анализируешь задачу только с точки зрения реализации.

Твоя задача:
- предложить шаги реализации;
- определить основные классы/компоненты;
- указать порядок работ;
- не писать финальный результат.

Не переходи к EXECUTION.
Отвечай на русском языке."""
    }
}
