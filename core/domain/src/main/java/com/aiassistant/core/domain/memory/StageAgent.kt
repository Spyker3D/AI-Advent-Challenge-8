package com.aiassistant.core.domain.memory

import android.util.Log
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.invariant.Invariant
import com.aiassistant.core.domain.invariant.InvariantResponsePolicy
import com.aiassistant.core.domain.invariant.InvariantValidationResult
import com.aiassistant.core.domain.invariant.InvariantValidator
import com.aiassistant.core.domain.repository.InvariantRepository
import java.util.UUID

class StageAgent(
    val stage: TaskStage,
    val systemPrompt: String,
    private val llmClient: LlmClient,
    private val invariantRepository: InvariantRepository,
    private val invariantValidator: InvariantValidator,
    private val promptBuilder: PromptBuilder
) {
    suspend fun run(
        taskContext: TaskContext,
        longTermMemory: LongTermMemory
    ): AgentRunResult {
        val result = send(
            taskContext = taskContext,
            longTermMemory = longTermMemory,
            operationPrompt = null,
            userMessage = taskContext.description
        )
        return ensureValidationReport(taskContext, longTermMemory, result)
    }

    suspend fun applyEdit(
        taskContext: TaskContext,
        longTermMemory: LongTermMemory,
        feedback: String
    ): AgentRunResult {
        val result = send(
            taskContext = taskContext,
            longTermMemory = longTermMemory,
            operationPrompt = editPrompt(taskContext, feedback),
            userMessage = feedback
        )
        return ensureValidationReport(taskContext, longTermMemory, result)
    }

    suspend fun reviseExecutionDuringValidation(
        taskContext: TaskContext,
        longTermMemory: LongTermMemory,
        feedback: String
    ): AgentRunResult {
        require(stage == TaskStage.EXECUTION) {
            "Only ExecutionAgent can revise executionResult"
        }
        require(taskContext.taskState.stage == TaskStage.VALIDATION) {
            "Execution revision is only allowed during VALIDATION"
        }

        return send(
            taskContext = taskContext,
            longTermMemory = longTermMemory,
            operationPrompt = """Ты ExecutionAgent.

Сейчас задача находится на этапе VALIDATION, но пользователь попросил внести правку в итоговый результат.

Твоя задача:
- взять текущий executionResult;
- применить пользовательскую правку;
- сохранить структуру документа;
- не писать validation report;
- не переходить к DONE;
- вернуть полный обновлённый executionResult;
- отвечать на русском языке.

Текущий executionResult:
${taskContext.executionResult}

Validation report:
${taskContext.validationResult}

User edit request:
$feedback""",
            userMessage = feedback,
            requiredContextStage = TaskStage.VALIDATION
        )
    }

    suspend fun answerQuestion(
        taskContext: TaskContext,
        longTermMemory: LongTermMemory,
        question: String
    ): String {
        val result = send(
            taskContext = taskContext,
            longTermMemory = longTermMemory,
            operationPrompt = """Ты отвечаешь на уточняющий вопрос о текущем этапе задачи.

Важно:
- ответь только на вопрос пользователя;
- не переписывай полный результат этапа;
- не обновляй результат этапа;
- не переходи к другому этапу;
- отвечай кратко и на русском языке.

Текущий этап: ${taskContext.taskState.stage.name}

Текущий результат этапа:
${currentStageResult(taskContext)}

Вопрос пользователя:
$question""",
            userMessage = question
        )
        return when (result) {
            is AgentRunResult.Success -> result.content
            is AgentRunResult.BlockedByInvariants -> result.message
        }
    }

    private suspend fun send(
        taskContext: TaskContext,
        longTermMemory: LongTermMemory,
        operationPrompt: String?,
        userMessage: String,
        requiredContextStage: TaskStage = stage
    ): AgentRunResult {
        require(taskContext.taskState.stage == requiredContextStage) {
            "Agent $stage cannot run this operation at ${taskContext.taskState.stage}"
        }

        Log.d("STAGE_AGENT", "stage=$stage")
        Log.d("STAGE_AGENT", "profileChars=${longTermMemory.profile.length}")
        Log.d("STAGE_AGENT", "preferencesChars=${longTermMemory.preferences.length}")
        Log.d("STAGE_AGENT", "globalRulesChars=${longTermMemory.globalRules.length}")
        Log.d("STAGE_AGENT", "languageRule=Russian")
        val invariants = invariantRepository.getInvariants()
        Log.d("INVARIANTS", "loaded=${invariants.size}")
        invariants.forEach { Log.d("INVARIANTS", it.description) }

        val messages = listOf(
            Message(
                id = UUID.randomUUID().toString(),
                content = buildSystemMessage(
                    taskContext,
                    longTermMemory,
                    operationPrompt,
                    invariants
                ),
                role = MessageRole.SYSTEM
            ),
            Message(
                id = UUID.randomUUID().toString(),
                content = userMessage,
                role = MessageRole.USER
            )
        )

        return sendValidated(messages, invariants)
    }

    private fun buildSystemMessage(
        taskContext: TaskContext,
        longTermMemory: LongTermMemory,
        operationPrompt: String?,
        invariants: List<Invariant>
    ): String = buildString {
        appendLine("ВАЖНОЕ ПРАВИЛО ЯЗЫКА:")
        appendLine("Всегда отвечай на русском языке, если пользователь явно не попросил другой язык.")
        appendLine()
        appendMemorySection("USER_PROFILE", longTermMemory.profile)
        appendMemorySection("USER_PREFERENCES", longTermMemory.preferences)
        appendMemorySection("GLOBAL_RULES", longTermMemory.globalRules)
        appendMemorySection("PROJECT_KNOWLEDGE", longTermMemory.projectKnowledge)
        appendMemorySection("DECISIONS", longTermMemory.decisions)
        appendLine("=========================")
        appendLine("STAGE_AGENT_PROMPT")
        appendLine("=========================")
        appendLine(
            systemPrompt
                .replace("{planningResult}", taskContext.planningResult)
                .replace("{executionResult}", taskContext.executionResult)
                .replace("{validationResult}", taskContext.validationResult)
                .replace("{taskContext.description}", taskContext.description)
        )
        appendLine()
        appendLine("=========================")
        appendLine("TASK_CONTEXT")
        appendLine("=========================")
        appendLine("Task ID: ${taskContext.id}")
        appendLine("Title: ${taskContext.title}")
        appendLine("Description: ${taskContext.description}")
        appendLine("Goals:")
        appendLine(taskContext.goals.joinToString("\n"))
        appendLine("Constraints:")
        appendLine(taskContext.constraints.joinToString("\n"))
        appendLine("Decisions:")
        appendLine(taskContext.decisions.joinToString("\n"))
        appendLine("Current stage: ${taskContext.taskState.stage.name}")
        appendLine("Current status: ${taskContext.taskState.status.name}")
        appendLine("Planning result:")
        appendLine(taskContext.planningResult)
        appendLine("Execution result:")
        appendLine(taskContext.executionResult)
        appendLine("Validation result:")
        appendLine(taskContext.validationResult)

        if (stage == TaskStage.VALIDATION) {
            appendLine()
            appendLine("=========================")
            appendLine("VALIDATION_INPUT")
            appendLine("=========================")
            appendLine("Task description:")
            appendLine(taskContext.description)
            appendLine("Goals:")
            taskContext.goals.forEach { appendLine("- $it") }
            appendLine("Constraints:")
            taskContext.constraints.forEach { appendLine("- $it") }
            appendLine("Planning result:")
            appendLine(taskContext.planningResult)
            appendLine("Execution result to validate:")
            appendLine(taskContext.executionResult)
        }

        if (!operationPrompt.isNullOrBlank()) {
            appendLine()
            appendLine("=========================")
            appendLine("CURRENT_OPERATION")
            appendLine("=========================")
            appendLine(operationPrompt)
        }
        appendLine()
        append(promptBuilder.buildInvariantSection(invariants))
    }

    private suspend fun sendValidated(
        messages: List<Message>,
        invariants: List<Invariant>
    ): AgentRunResult {
        val firstResponse = llmClient.sendChat(messages, maxTokens = null).getOrThrow().message
        val firstValidation = invariantValidator.validateResponse(firstResponse, invariants)
        Log.d("INVARIANTS", "validation=${firstValidation::class.simpleName}")
        if (firstValidation is InvariantValidationResult.Pass) {
            return AgentRunResult.Success(firstValidation.response)
        }

        firstValidation as InvariantValidationResult.Fail
        Log.w("INVARIANTS", "violations=${firstValidation.violations.joinToString()}")
        val retryMessages = messages + listOf(
            Message(
                id = UUID.randomUUID().toString(),
                content = firstValidation.originalResponse,
                role = MessageRole.ASSISTANT
            ),
            Message(
                id = UUID.randomUUID().toString(),
                content = InvariantResponsePolicy.retryPrompt(firstValidation),
                role = MessageRole.USER
            )
        )
        val retryResponse = llmClient.sendChat(retryMessages, maxTokens = null)
            .getOrThrow()
            .message
        val retryValidation = invariantValidator.validateResponse(retryResponse, invariants)
        Log.d("INVARIANTS", "validation=${retryValidation::class.simpleName}")
        return when (retryValidation) {
            is InvariantValidationResult.Pass -> AgentRunResult.Success(retryValidation.response)
            is InvariantValidationResult.Fail -> {
                Log.w("INVARIANTS", "violations=${retryValidation.violations.joinToString()}")
                AgentRunResult.BlockedByInvariants(
                    message = InvariantResponsePolicy.safeRefusal(
                        retryValidation.violations,
                        invariants
                    ),
                    violations = retryValidation.violations
                )
            }
        }
    }

    private fun StringBuilder.appendMemorySection(title: String, content: String) {
        appendLine("=========================")
        appendLine(title)
        appendLine("=========================")
        appendLine(content)
        appendLine()
    }

    private fun editPrompt(taskContext: TaskContext, feedback: String): String =
        when (stage) {
            TaskStage.PLANNING -> """Ты PlanningAgent.

Пользователь запросил изменения текущего результата планирования.

Текущий planningResult:
${taskContext.planningResult}

Запрос на изменение:
$feedback

Обнови planningResult согласно запросу пользователя.
Верни только полный обновлённый planningResult.
Отвечай на русском языке."""

            TaskStage.EXECUTION -> """Ты ExecutionAgent.

Пользователь запросил изменения текущего результата выполнения.

Утверждённый planningResult:
${taskContext.planningResult}

Текущий executionResult:
${taskContext.executionResult}

Запрос на изменение:
$feedback

Обнови executionResult согласно запросу пользователя.
Верни только полный обновлённый executionResult.
Отвечай на русском языке."""

            TaskStage.VALIDATION -> """Ты ValidationAgent.

Пользователь запросил изменения текущего результата проверки.

Execution result:
${taskContext.executionResult}

Текущий validationResult:
${taskContext.validationResult}

Запрос на изменение:
$feedback

Обнови validationResult согласно запросу пользователя.
Сохрани обязательный формат # Validation Report со всеми секциями:
Статус, Что выполнено хорошо, Замечания, Риски, Рекомендации, Итог.
Не копируй executionResult и не пиши новое ТЗ.
Верни только полный обновлённый validationResult.
Отвечай на русском языке."""

            TaskStage.DONE -> "Задача завершена. Не изменяй результаты этапов."
        }

    private fun currentStageResult(taskContext: TaskContext): String = when (stage) {
        TaskStage.PLANNING -> taskContext.planningResult
        TaskStage.EXECUTION -> taskContext.executionResult
        TaskStage.VALIDATION -> taskContext.validationResult
        TaskStage.DONE -> taskContext.currentState
    }

    private suspend fun ensureValidationReport(
        taskContext: TaskContext,
        longTermMemory: LongTermMemory,
        result: AgentRunResult
    ): AgentRunResult {
        if (result is AgentRunResult.BlockedByInvariants) return result
        val content = (result as AgentRunResult.Success).content
        if (stage != TaskStage.VALIDATION ||
            ValidationReportValidator.isValid(content, taskContext.executionResult)
        ) {
            return result
        }

        Log.w(
            "VALIDATION_AGENT",
            "Validation result is invalid or looks like copied executionResult; retrying"
        )
        val retryResult = send(
            taskContext = taskContext,
            longTermMemory = longTermMemory,
            operationPrompt = VALIDATION_RETRY_PROMPT,
            userMessage = "Сформируй корректный отчёт проверки executionResult."
        )
        if (retryResult is AgentRunResult.BlockedByInvariants) return retryResult
        val retryContent = (retryResult as AgentRunResult.Success).content
        if (ValidationReportValidator.isValid(retryContent, taskContext.executionResult)) {
            return retryResult
        }

        Log.w("VALIDATION_AGENT", "Validation retry still violates report requirements")
        val invariants = invariantRepository.getInvariants()
        return AgentRunResult.Success(
            VALIDATION_FALLBACK_REPORT + "\n\n" +
                InvariantResponsePolicy.complianceStatement(invariants)
        )
    }

    private companion object {
        const val VALIDATION_RETRY_PROMPT = """Ты ошибся: предыдущий ответ скопировал executionResult или нарушил формат.

Нужно сделать именно validation report.

Не копируй ТЗ.
Не переписывай ТЗ.
Найди минимум 2 замечания или риска.
Отвечай на русском языке.
Верни только отчёт строго по формату:

# Validation Report

## Статус
OK, OK_WITH_RECOMMENDATIONS или NEEDS_CHANGES

## Что выполнено хорошо
- ...

## Замечания
- ...

## Риски
- ...

## Рекомендации
1. ...

## Итог
Короткий вывод."""

        const val VALIDATION_FALLBACK_REPORT = """# Validation Report

## Статус
NEEDS_CHANGES

## Что выполнено хорошо
- Результат этапа EXECUTION сформирован и доступен для проверки.

## Замечания
- Автоматическая проверка не смогла сформировать корректный структурированный отчёт.
- Требуется ручная проверка покрытия planningResult и ограничений TaskContext.

## Риски
- Возможны пропущенные edge cases.
- Возможны невыявленные противоречия или неполные критерии готовности.

## Рекомендации
1. Повторить этап VALIDATION.
2. Вручную проверить цели, ограничения, edge cases и критерии готовности.

## Итог
Завершать задачу без дополнительной проверки не рекомендуется."""
    }
}
