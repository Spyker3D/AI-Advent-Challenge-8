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
            val withSwarmResults = taskContext.copy(
                planningSwarmResults = output.swarmResults,
                planningResult = output.finalPlanningResult
            )
            val completed = taskStateMachine.completeStage(
                withSwarmResults,
                output.finalPlanningResult
            )
            save(completed)
            return completed
        }

        val longTermMemory = longTermMemoryRepository.getLongTermMemory()
        val result = if (stage == TaskStage.PLANNING && userInput != null) {
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
        val updated = saveStageResult(taskContext, stage, result)
        save(updated)
        return updated
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
                    TaskWaitingUserResult.ContextUpdated(
                        taskContext = applyValidationEditToExecutionResult(
                            chatId,
                            taskContext,
                            userText
                        ),
                        resultWasEdited = true,
                        executionRevisedDuringValidation = true
                    )
                } else {
                    TaskWaitingUserResult.ContextUpdated(
                        taskContext = applyFeedbackToCurrentStage(
                            chatId,
                            taskContext,
                            userText
                        ),
                        resultWasEdited = true
                    )
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
            val readyForRevalidation = running.copy(
                executionResult = updatedExecution,
                validationResult = "",
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
            val validated = taskStateMachine.completeStage(
                readyForRevalidation,
                validationReport
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
}
