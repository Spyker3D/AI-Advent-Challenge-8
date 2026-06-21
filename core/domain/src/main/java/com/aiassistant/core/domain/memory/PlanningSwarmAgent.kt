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

class PlanningSwarmAgent(
    private val role: PlanningSwarmRole,
    private val systemPrompt: String,
    private val llmClient: LlmClient,
    private val invariantRepository: InvariantRepository,
    private val invariantValidator: InvariantValidator,
    private val promptBuilder: PromptBuilder
) {
    suspend fun run(
        taskContext: TaskContext,
        longTermMemory: LongTermMemory
    ): PlanningSwarmResult {
        require(taskContext.taskState.stage == TaskStage.PLANNING) {
            "Planning swarm agent cannot run at ${taskContext.taskState.stage}"
        }
        Log.d("PLANNING_SWARM", "agent=$role started")
        val invariants = invariantRepository.getInvariants()
        val messages = planningMessages(
            systemContent = buildPlanningSystemPrompt(
                rolePrompt = systemPrompt,
                taskContext = taskContext,
                longTermMemory = longTermMemory,
                invariants = invariants,
                promptBuilder = promptBuilder
            ),
            userContent = taskContext.description
        )
        val agentResult = sendPlanningResponse(
            messages = messages,
            invariants = invariants,
            llmClient = llmClient,
            invariantValidator = invariantValidator
        )
        return when (agentResult) {
            is AgentRunResult.Success -> {
                Log.d(
                    "PLANNING_SWARM",
                    "agent=$role completed chars=${agentResult.content.length}"
                )
                PlanningSwarmResult(
                    role = role,
                    title = titleFor(role),
                    content = agentResult.content
                )
            }
            is AgentRunResult.BlockedByInvariants -> {
                Log.w("PLANNING_SWARM", "agent=$role blocked by invariants")
                PlanningSwarmResult(
                    role = role,
                    title = titleFor(role),
                    content = agentResult.message,
                    blockedByInvariants = true,
                    violations = agentResult.violations
                )
            }
        }
    }

    private fun titleFor(role: PlanningSwarmRole): String = when (role) {
        PlanningSwarmRole.REQUIREMENTS -> "RequirementsPlanningAgent"
        PlanningSwarmRole.ARCHITECTURE -> "ArchitecturePlanningAgent"
        PlanningSwarmRole.RISKS -> "RiskPlanningAgent"
        PlanningSwarmRole.TESTING -> "TestingPlanningAgent"
        PlanningSwarmRole.IMPLEMENTATION -> "ImplementationPlanningAgent"
        PlanningSwarmRole.SUPERVISOR -> "PlanningSupervisorAgent"
    }
}

internal fun buildPlanningSystemPrompt(
    rolePrompt: String,
    taskContext: TaskContext,
    longTermMemory: LongTermMemory,
    invariants: List<Invariant>,
    promptBuilder: PromptBuilder,
    extraContent: String = ""
): String = buildString {
    appendLine(rolePrompt)
    appendLine()
    appendPlanningSection("USER_PROFILE", longTermMemory.profile)
    appendPlanningSection("USER_PREFERENCES", longTermMemory.preferences)
    appendPlanningSection("GLOBAL_RULES", longTermMemory.globalRules)
    appendPlanningSection("PROJECT_KNOWLEDGE", longTermMemory.projectKnowledge)
    appendPlanningSection("DECISIONS", longTermMemory.decisions)
    appendLine("=========================")
    appendLine("TASK_CONTEXT")
    appendLine("=========================")
    appendLine("Task ID: ${taskContext.id}")
    appendLine("Title: ${taskContext.title}")
    appendLine("Description: ${taskContext.description}")
    appendLine("Goals:")
    taskContext.goals.forEach { appendLine("- $it") }
    appendLine("Constraints:")
    taskContext.constraints.forEach { appendLine("- $it") }
    appendLine("Decisions:")
    taskContext.decisions.forEach { appendLine("- $it") }
    appendLine("Current stage: PLANNING")
    if (extraContent.isNotBlank()) {
        appendLine()
        appendLine(extraContent)
    }
    append(promptBuilder.buildInvariantSection(invariants))
}

private fun StringBuilder.appendPlanningSection(title: String, content: String) {
    appendLine("=========================")
    appendLine(title)
    appendLine("=========================")
    appendLine(content)
    appendLine()
}

internal fun planningMessages(
    systemContent: String,
    userContent: String
): List<Message> = listOf(
    Message(
        id = UUID.randomUUID().toString(),
        content = systemContent,
        role = MessageRole.SYSTEM
    ),
    Message(
        id = UUID.randomUUID().toString(),
        content = userContent,
        role = MessageRole.USER
    )
)

internal suspend fun sendPlanningResponse(
    messages: List<Message>,
    invariants: List<Invariant>,
    llmClient: LlmClient,
    invariantValidator: InvariantValidator
): AgentRunResult {
    val first = llmClient.sendChat(messages, maxTokens = null).getOrThrow().message
    val firstValidation = invariantValidator.validateResponse(first, invariants)
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
    val retry = llmClient.sendChat(retryMessages, maxTokens = null).getOrThrow().message
    return when (val validation = invariantValidator.validateResponse(retry, invariants)) {
        is InvariantValidationResult.Pass -> AgentRunResult.Success(validation.response)
        is InvariantValidationResult.Fail -> {
            Log.w("INVARIANTS", "violations=${validation.violations.joinToString()}")
            AgentRunResult.BlockedByInvariants(
                message = InvariantResponsePolicy.safeRefusal(
                    validation.violations,
                    invariants
                ),
                violations = validation.violations
            )
        }
    }
}
