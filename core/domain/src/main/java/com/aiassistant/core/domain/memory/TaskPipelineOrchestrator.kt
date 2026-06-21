package com.aiassistant.core.domain.memory

import com.aiassistant.core.domain.repository.LongTermMemoryRepository
import com.aiassistant.core.domain.repository.WorkingMemoryRepository
import java.util.UUID
import javax.inject.Inject

class TaskPipelineOrchestrator @Inject constructor(
    private val workingMemoryRepository: WorkingMemoryRepository,
    private val longTermMemoryRepository: LongTermMemoryRepository,
    private val taskStateMachine: TaskStateMachine,
    private val planningSwarmOrchestrator: PlanningSwarmOrchestrator,
    private val planningSupervisorAgent: PlanningSupervisorAgent,
    stageAgentFactory: StageAgentFactory
) {
    private val planningAgent = stageAgentFactory.create(TaskStage.PLANNING)
    private val executionAgent = stageAgentFactory.create(TaskStage.EXECUTION)
    private val validationAgent = stageAgentFactory.create(TaskStage.VALIDATION)
    private val doneAgent = stageAgentFactory.create(TaskStage.DONE)

    suspend fun startTask(chatId: String, taskContextIdOrRequest: String): TaskContext {
        val existing = workingMemoryRepository.getTaskContext(taskContextIdOrRequest)
        val taskContext = (existing ?: TaskContext(
            id = "task_${UUID.randomUUID()}",
            title = taskContextIdOrRequest.lineSequence().firstOrNull().orEmpty().take(80),
            description = taskContextIdOrRequest,
            relatedChatIds = listOf(chatId)
        )).copy(
            relatedChatIds = (existing?.relatedChatIds.orEmpty() + chatId).distinct(),
            taskState = TaskState(
                stage = TaskStage.PLANNING,
                status = TaskRunStatus.RUNNING,
                currentStep = "Planning swarm is running"
            ),
            planningResult = "",
            executionResult = "",
            validationResult = "",
            planningSwarmResults = emptyList(),
            blockedByInvariantsMessage = "",
            currentState = "",
            updatedAt = System.currentTimeMillis()
        )
        save(taskContext)
        return runCurrentStage(chatId, taskContext.id)
    }

    suspend fun continueTask(chatId: String, taskContextId: String): TaskContext {
        val taskContext = requireTask(taskContextId).withChat(chatId)
        if (taskContext.taskState.status != TaskRunStatus.RUNNING) return taskContext
        return runCurrentStage(chatId, taskContext.id)
    }

    suspend fun runCurrentStage(
        chatId: String,
        taskContextId: String,
        userInput: String? = null
    ): TaskContext {
        val taskContext = requireTask(taskContextId).withChat(chatId)
        if (taskContext.taskState.status != TaskRunStatus.RUNNING) return taskContext

        val stage = taskContext.taskState.stage
        if (stage == TaskStage.PLANNING && userInput == null) {
            val output = planningSwarmOrchestrator.runPlanning(taskContext)
            return when (val finalPlan = output.finalPlanningResult) {
                is AgentRunResult.Success -> {
                    val withSwarmResults = taskContext.copy(
                        planningSwarmResults = output.swarmResults,
                        planningResult = finalPlan.content,
                        blockedByInvariantsMessage = ""
                    )
                    val completed = taskStateMachine.completeStage(
                        withSwarmResults,
                        finalPlan.content
                    )
                    save(completed)
                    completed
                }
                is AgentRunResult.BlockedByInvariants -> {
                    val blocked = blockByInvariants(
                        taskContext.copy(planningSwarmResults = output.swarmResults),
                        finalPlan
                    )
                    save(blocked)
                    blocked
                }
            }
        }

        val longTermMemory = longTermMemoryRepository.getLongTermMemory()
        val result: AgentRunResult = if (stage == TaskStage.PLANNING && userInput != null) {
            planningSupervisorAgent.revisePlan(
                taskContext = taskContext,
                longTermMemory = longTermMemory,
                feedback = userInput
            )
        } else if (userInput == null) {
            agentFor(stage).run(taskContext, longTermMemory)
        } else {
            agentFor(stage).applyEdit(taskContext, longTermMemory, userInput)
        }
        return when (result) {
            is AgentRunResult.Success -> {
                val updated = saveStageResult(
                    taskContext.copy(blockedByInvariantsMessage = ""),
                    stage,
                    result.content
                )
                save(updated)
                updated
            }
            is AgentRunResult.BlockedByInvariants -> {
                val blocked = blockByInvariants(taskContext, result)
                save(blocked)
                blocked
            }
        }
    }

    suspend fun pauseTask(taskContextId: String): TaskContext {
        val updated = taskStateMachine.pause(requireTask(taskContextId))
        save(updated)
        return updated
    }

    suspend fun resumeTask(chatId: String, taskContextId: String): TaskContext {
        val resumed = taskStateMachine.resume(requireTask(taskContextId).withChat(chatId))
        save(resumed)
        return if (resumed.taskState.status == TaskRunStatus.RUNNING) {
            continueTask(chatId, taskContextId)
        } else {
            resumed
        }
    }

    suspend fun confirmNextStage(chatId: String, taskContextId: String): TaskContext {
        val current = requireTask(taskContextId).withChat(chatId)
        if (current.taskState.status != TaskRunStatus.WAITING_USER) return current
        if (current.taskState.currentStep == "Blocked by invariants") return current

        val moved = taskStateMachine.moveToNextStage(current)
        save(moved)
        return runCurrentStage(chatId, moved.id)
    }

    suspend fun requestStageTransition(
        taskContextId: String,
        requestedStage: TaskStage
    ): TaskTransitionResult {
        val current = requireTask(taskContextId)
        val currentStage = current.taskState.stage

        if (current.taskState.status != TaskRunStatus.WAITING_USER) {
            return TaskTransitionResult.Blocked(
                message = "Переход недоступен: текущий статус " +
                    "${current.taskState.status.name}. Сначала завершите или возобновите этап.",
                taskContext = current
            )
        }

        if (current.taskState.currentStep == "Blocked by invariants") {
            return TaskTransitionResult.Blocked(
                message = current.blockedByInvariantsMessage,
                taskContext = current
            )
        }

        if (!taskStateMachine.canTransition(currentStage, requestedStage)) {
            return TaskTransitionResult.Blocked(
                message = "Нельзя перейти из ${currentStage.name} в ${requestedStage.name}. " +
                    "Следующий разрешённый этап: " +
                    "${taskStateMachine.nextStage(currentStage)?.name ?: "нет"}.",
                taskContext = current
            )
        }

        val updated = taskStateMachine.moveToNextStage(current)
        save(updated)
        return TaskTransitionResult.Allowed(updated)
    }

    suspend fun handleWaitingUserInput(
        chatId: String,
        taskContextId: String,
        userText: String
    ): TaskWaitingUserResult {
        val taskContext = requireTask(taskContextId).withChat(chatId)
        if (taskContext.taskState.status != TaskRunStatus.WAITING_USER) {
            return TaskWaitingUserResult.ContextUpdated(taskContext)
        }

        if (TaskUserIntentParser.isConfirmation(userText)) {
            if (taskContext.taskState.currentStep == "Blocked by invariants") {
                return TaskWaitingUserResult.Blocked(
                    taskContext.blockedByInvariantsMessage,
                    taskContext
                )
            }
            return TaskWaitingUserResult.ContextUpdated(
                confirmNextStage(chatId, taskContextId)
            )
        }

        TaskUserIntentParser.parseRequestedStage(userText)?.let { requestedStage ->
            return when (
                val transition = requestStageTransition(taskContextId, requestedStage)
            ) {
                is TaskTransitionResult.Allowed ->
                    TaskWaitingUserResult.ContextUpdated(
                        continueTask(chatId, transition.taskContext.id)
                    )
                is TaskTransitionResult.Blocked -> TaskWaitingUserResult.Blocked(
                    transition.message,
                    transition.taskContext
                )
            }
        }

        return when {
            TaskUserIntentParser.isEditRequest(userText) ->
                if (taskContext.taskState.stage == TaskStage.VALIDATION) {
                    applyValidationEditToExecutionResult(
                        chatId,
                        taskContext,
                        userText
                    ).toWaitingUserResult(
                        resultWasEdited = true,
                        executionRevisedDuringValidation = true
                    )
                } else {
                    val updated = applyFeedbackToCurrentStage(
                        chatId,
                        taskContext,
                        userText
                    )
                    updated.toWaitingUserResult(resultWasEdited = true)
                }

            TaskUserIntentParser.isClarifyingQuestion(userText) ->
                answerQuestionResult(chatId, taskContext, userText)

            else -> answerQuestionResult(chatId, taskContext, userText)
        }
    }

    suspend fun applyFeedbackToCurrentStage(
        chatId: String,
        taskContext: TaskContext,
        feedback: String
    ): TaskContext {
        if (taskContext.taskState.status != TaskRunStatus.WAITING_USER ||
            taskContext.taskState.stage == TaskStage.DONE
        ) {
            return taskContext
        }

        val running = taskStateMachine.beginFeedback(taskContext.withChat(chatId))
        save(running)

        return try {
            runCurrentStage(chatId, running.id, feedback)
        } catch (throwable: Throwable) {
            save(taskStateMachine.waitForUser(running))
            throw throwable
        }
    }

    suspend fun applyValidationEditToExecutionResult(
        chatId: String,
        taskContext: TaskContext,
        feedback: String
    ): TaskContext {
        if (taskContext.taskState.stage != TaskStage.VALIDATION ||
            taskContext.taskState.status != TaskRunStatus.WAITING_USER
        ) {
            return taskContext
        }

        val running = taskStateMachine.beginFeedback(taskContext.withChat(chatId))
        save(running)

        return try {
            val longTermMemory = longTermMemoryRepository.getLongTermMemory()
            val updatedExecution = executionAgent.reviseExecutionDuringValidation(
                taskContext = running,
                longTermMemory = longTermMemory,
                feedback = feedback
            )
            if (updatedExecution is AgentRunResult.BlockedByInvariants) {
                val blocked = blockByInvariants(running, updatedExecution)
                save(blocked)
                return blocked
            }
            val readyForRevalidation = running.copy(
                executionResult = (updatedExecution as AgentRunResult.Success).content,
                validationResult = "",
                blockedByInvariantsMessage = "",
                taskState = running.taskState.copy(
                    status = TaskRunStatus.RUNNING,
                    currentStep = "Validation agent is rechecking updated execution result"
                ),
                updatedAt = System.currentTimeMillis()
            )
            save(readyForRevalidation)

            val validationReport = validationAgent.run(
                taskContext = readyForRevalidation,
                longTermMemory = longTermMemory
            )
            if (validationReport is AgentRunResult.BlockedByInvariants) {
                val blocked = blockByInvariants(readyForRevalidation, validationReport)
                save(blocked)
                return blocked
            }
            val validated = taskStateMachine.completeStage(
                readyForRevalidation,
                (validationReport as AgentRunResult.Success).content
            )
            save(validated)
            validated
        } catch (throwable: Throwable) {
            save(taskStateMachine.waitForUser(taskContext))
            throw throwable
        }
    }

    suspend fun answerQuestionAboutCurrentStage(
        chatId: String,
        taskContext: TaskContext,
        question: String
    ): String {
        val current = taskContext.withChat(chatId)
        require(current.taskState.status == TaskRunStatus.WAITING_USER) {
            "Questions about a stage can only be answered in WAITING_USER"
        }
        val longTermMemory = longTermMemoryRepository.getLongTermMemory()
        return agentFor(current.taskState.stage).answerQuestion(
            taskContext = current,
            longTermMemory = longTermMemory,
            question = question
        )
    }

    private suspend fun answerQuestionResult(
        chatId: String,
        taskContext: TaskContext,
        question: String
    ): TaskWaitingUserResult.QuestionAnswered =
        TaskWaitingUserResult.QuestionAnswered(
            answer = answerQuestionAboutCurrentStage(chatId, taskContext, question),
            taskContext = taskContext
        )

    private fun agentFor(stage: TaskStage): StageAgent = when (stage) {
        TaskStage.PLANNING -> planningAgent
        TaskStage.EXECUTION -> executionAgent
        TaskStage.VALIDATION -> validationAgent
        TaskStage.DONE -> doneAgent
    }

    private fun saveStageResult(
        taskContext: TaskContext,
        stage: TaskStage,
        result: String
    ): TaskContext {
        require(taskContext.taskState.stage == stage) {
            "Cannot save $stage result for ${taskContext.taskState.stage}"
        }
        return taskStateMachine.completeStage(taskContext, result)
    }

    private fun blockByInvariants(
        taskContext: TaskContext,
        result: AgentRunResult.BlockedByInvariants
    ): TaskContext = taskStateMachine.blockByInvariants(taskContext, result.message)

    private suspend fun requireTask(id: String): TaskContext =
        requireNotNull(workingMemoryRepository.getTaskContext(id)) {
            "Task context not found: $id"
        }

    private suspend fun save(taskContext: TaskContext) {
        workingMemoryRepository.saveTaskContext(taskContext)
        workingMemoryRepository.setActiveTaskContext(taskContext.id)
    }

    private fun TaskContext.withChat(chatId: String): TaskContext {
        if (chatId in relatedChatIds) return this
        return copy(relatedChatIds = relatedChatIds + chatId)
    }

    private fun TaskContext.toWaitingUserResult(
        resultWasEdited: Boolean = false,
        executionRevisedDuringValidation: Boolean = false
    ): TaskWaitingUserResult =
        if (taskState.currentStep == "Blocked by invariants") {
            TaskWaitingUserResult.Blocked(blockedByInvariantsMessage, this)
        } else {
            TaskWaitingUserResult.ContextUpdated(
                taskContext = this,
                resultWasEdited = resultWasEdited,
                executionRevisedDuringValidation = executionRevisedDuringValidation
            )
        }
}
