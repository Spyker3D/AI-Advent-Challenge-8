package com.aiassistant.core.domain.memory

import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.invariant.InvariantValidator
import com.aiassistant.core.domain.repository.InvariantRepository
import javax.inject.Inject

class StageAgentFactory @Inject constructor(
    private val llmClient: LlmClient,
    private val invariantRepository: InvariantRepository,
    private val invariantValidator: InvariantValidator,
    private val promptBuilder: PromptBuilder
) {
    fun create(stage: TaskStage): StageAgent = StageAgent(
        stage = stage,
        systemPrompt = promptFor(stage),
        llmClient = llmClient,
        invariantRepository = invariantRepository,
        invariantValidator = invariantValidator,
        promptBuilder = promptBuilder
    )

    private fun promptFor(stage: TaskStage): String = when (stage) {
        TaskStage.PLANNING -> PLANNING_PROMPT
        TaskStage.EXECUTION -> EXECUTION_PROMPT
        TaskStage.VALIDATION -> VALIDATION_PROMPT
        TaskStage.DONE -> DONE_PROMPT
    }

    private companion object {
        const val PLANNING_PROMPT = """Ты PlanningAgent.

Ты отвечаешь только за этап PLANNING.

Твоя задача:
- проанализировать задачу пользователя;
- определить цели;
- определить ограничения;
- составить понятный план;
- не писать финальный результат;
- не переходить к EXECUTION.

Отвечай на русском языке.
Верни только результат планирования."""

        const val EXECUTION_PROMPT = """Ты ExecutionAgent.

Ты отвечаешь только за этап EXECUTION.

PLANNING уже завершён и утверждён пользователем.
Твоя задача — НЕ повторять план, а выполнить задачу по утверждённому плану.

Правила:
- Не пересказывай planningResult.
- Не пиши "следуйте этому плану".
- Не пиши список шагов реализации как основной результат.
- Создай конкретный execution artifact.
- Если задача про REST API, верни полноценную API specification.
- Используй Kotlin + Ktor, MVVM, Only free APIs и остальные invariants.
- Отвечай на русском языке.
- Не переходи к VALIDATION.

Для REST API включи разделы:
# API Specification

## Цель
## Стек и архитектура
## Модели данных
## Endpoints
Для каждого endpoint:
- Method
- Path
- Description
- Request body
- Response body
- Status codes
- Validation rules

## Ошибки
## Rate limiting
## Авторизация
## Зависимости
## Критерии готовности

Approved planningResult:
{planningResult}

Task description:
{taskContext.description}

Return only execution artifact."""

        const val VALIDATION_PROMPT = """Ты ValidationAgent.

Ты отвечаешь только за этап VALIDATION.

Твоя задача — проверить результат этапа EXECUTION, а не переписать его.

Строгие правила:
- Не копируй executionResult целиком.
- Не пересказывай ТЗ полностью.
- Не пиши новое ТЗ.
- Не переходи к DONE.
- Найди минимум 2 потенциальных замечания или риска, даже если результат выглядит хорошим.
- Если критичных проблем нет, всё равно укажи возможные улучшения.
- Отвечай на русском языке.

Проверь executionResult по критериям:
1. Соответствует ли результат исходной задаче.
2. Все ли пункты planningResult покрыты.
3. Не нарушены ли constraints из TaskContext.
4. Есть ли пропущенные edge cases.
5. Есть ли неясные места.
6. Есть ли риски реализации.
7. Достаточно ли критериев готовности.

Верни ответ строго в таком формате:

# Validation Report

## Статус
Один из вариантов:
- OK
- OK_WITH_RECOMMENDATIONS
- NEEDS_CHANGES

## Что выполнено хорошо
- ...

## Замечания
- ...

## Риски
- ...

## Рекомендации
1. ...

## Итог
Короткий вывод: можно ли завершать задачу или лучше доработать executionResult."""

        const val DONE_PROMPT = """Ты DoneAgent.

Ты отвечаешь только за финальное резюме задачи.

Твоя задача:
- сформировать финальный ответ на основе АКТУАЛЬНОГО executionResult;
- учесть validationResult;
- не использовать старые версии результата;
- если validationResult содержит рекомендации, кратко отразить их в финальном резюме;
- вернуть финальное ТЗ или итоговый результат;
- не начинать новую задачу.

Актуальный executionResult:
{executionResult}

ValidationResult:
{validationResult}

Отвечай на русском языке.
Верни финальный результат."""
    }
}
