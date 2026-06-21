package com.aiassistant.core.domain.memory

import android.util.Log
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.invariant.InvariantValidator
import com.aiassistant.core.domain.repository.InvariantRepository
import javax.inject.Inject

class PlanningSupervisorAgent @Inject constructor(
    private val llmClient: LlmClient,
    private val invariantRepository: InvariantRepository,
    private val invariantValidator: InvariantValidator,
    private val promptBuilder: PromptBuilder
) {
    suspend fun synthesize(
        taskContext: TaskContext,
        longTermMemory: LongTermMemory,
        swarmResults: List<PlanningSwarmResult>
    ): String {
        Log.d("PLANNING_SWARM", "supervisor started")
        val result = runSupervisor(
            taskContext = taskContext,
            longTermMemory = longTermMemory,
            operationPrompt = SYNTHESIZE_PROMPT,
            userContent = taskContext.description,
            swarmResults = swarmResults
        )
        Log.d("PLANNING_SWARM", "supervisor completed chars=${result.length}")
        return result
    }

    suspend fun revisePlan(
        taskContext: TaskContext,
        longTermMemory: LongTermMemory,
        feedback: String
    ): String = runSupervisor(
        taskContext = taskContext,
        longTermMemory = longTermMemory,
        operationPrompt = buildString {
            appendLine("Ты PlanningSupervisorAgent.")
            appendLine()
            appendLine("Пользователь попросил изменить итоговый planningResult.")
            appendLine("Не запускай новый этап.")
            appendLine("Не переходи к EXECUTION.")
            appendLine()
            appendLine("Текущий planningResult:")
            appendLine(taskContext.planningResult)
            appendLine()
            appendLine("User feedback:")
            appendLine(feedback)
            appendLine()
            appendLine("Обнови итоговый planningResult.")
            append("Верни полный обновлённый план.")
        },
        userContent = feedback,
        swarmResults = taskContext.planningSwarmResults
    )

    private suspend fun runSupervisor(
        taskContext: TaskContext,
        longTermMemory: LongTermMemory,
        operationPrompt: String,
        userContent: String,
        swarmResults: List<PlanningSwarmResult>
    ): String {
        require(taskContext.taskState.stage == TaskStage.PLANNING) {
            "Planning supervisor cannot run at ${taskContext.taskState.stage}"
        }
        val invariants = invariantRepository.getInvariants()
        val swarmSection = buildString {
            appendLine("=========================")
            appendLine("PLANNING_SWARM_RESULTS")
            appendLine("=========================")
            swarmResults.forEach { result ->
                appendLine()
                appendLine("## ${result.title} [${result.role}]")
                appendLine(result.content)
            }
        }
        val messages = planningMessages(
            systemContent = buildPlanningSystemPrompt(
                rolePrompt = operationPrompt,
                taskContext = taskContext,
                longTermMemory = longTermMemory,
                invariants = invariants,
                promptBuilder = promptBuilder,
                extraContent = swarmSection
            ),
            userContent = userContent
        )
        return sendPlanningResponse(
            messages = messages,
            invariants = invariants,
            llmClient = llmClient,
            invariantValidator = invariantValidator
        )
    }

    private companion object {
        const val SYNTHESIZE_PROMPT = """Ты PlanningSupervisorAgent.

Ты оркестратор этапа PLANNING.

У тебя есть исходная задача пользователя и предложения нескольких planning-агентов.

Твоя задача:
- прочитать предложения всех агентов;
- убрать дубли;
- разрешить противоречия;
- учесть invariants;
- сформировать единый итоговый planningResult;
- не переходить к EXECUTION;
- не писать финальный результат задачи.

Верни единый план в формате:

# Итоговый план

## Цель
...

## Требования
...

## Архитектура
...

## Шаги реализации
...

## Риски
...

## Тестирование
...

## Критерии готовности
..."""
    }
}
