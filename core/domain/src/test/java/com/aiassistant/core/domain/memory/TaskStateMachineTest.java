package com.aiassistant.core.domain.memory;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TaskStateMachineTest {
    private final TaskStateMachine stateMachine = new TaskStateMachine();

    @Test
    public void onlyAdjacentForwardTransitionsAreAllowed() {
        assertTrue(stateMachine.canTransition(TaskStage.PLANNING, TaskStage.EXECUTION));
        assertTrue(stateMachine.canTransition(TaskStage.EXECUTION, TaskStage.VALIDATION));
        assertTrue(stateMachine.canTransition(TaskStage.VALIDATION, TaskStage.DONE));
        assertFalse(stateMachine.canTransition(TaskStage.PLANNING, TaskStage.VALIDATION));
        assertFalse(stateMachine.canTransition(TaskStage.EXECUTION, TaskStage.DONE));
        assertFalse(stateMachine.canTransition(TaskStage.DONE, TaskStage.PLANNING));
        assertNull(stateMachine.nextStage(TaskStage.DONE));
    }

    @Test
    public void stageCompletionWaitsAfterValidationAndDoneCompletesTask() {
        TaskContext initial = task(TaskStage.PLANNING, TaskRunStatus.RUNNING);
        TaskContext planned = stateMachine.completeStage(initial, "plan");
        assertEquals("plan", planned.getPlanningResult());
        assertEquals(TaskRunStatus.WAITING_USER, planned.getTaskState().getStatus());

        TaskContext executing = stateMachine.moveToNextStage(planned);
        TaskContext executed = stateMachine.completeStage(executing, "result");
        assertEquals("result", executed.getExecutionResult());
        assertEquals(TaskStage.EXECUTION, executed.getTaskState().getStage());

        TaskContext validating = stateMachine.moveToNextStage(executed);
        TaskContext validated = stateMachine.completeStage(validating, "valid");
        assertEquals("valid", validated.getValidationResult());
        assertEquals(TaskStage.VALIDATION, validated.getTaskState().getStage());
        assertEquals(TaskRunStatus.WAITING_USER, validated.getTaskState().getStatus());

        TaskContext doneRunning = stateMachine.moveToNextStage(validated);
        assertEquals(TaskStage.DONE, doneRunning.getTaskState().getStage());
        assertEquals(TaskRunStatus.RUNNING, doneRunning.getTaskState().getStatus());

        TaskContext done = stateMachine.completeStage(doneRunning, "final summary");
        assertEquals("final summary", done.getCurrentState());
        assertEquals(TaskStage.DONE, done.getTaskState().getStage());
        assertEquals(TaskRunStatus.COMPLETED, done.getTaskState().getStatus());
    }

    @Test
    public void pauseAndResumePreserveStage() {
        TaskContext initial = task(TaskStage.EXECUTION, TaskRunStatus.RUNNING);
        TaskContext paused = stateMachine.pause(initial);
        TaskContext resumed = stateMachine.resume(paused);

        assertEquals(TaskStage.EXECUTION, paused.getTaskState().getStage());
        assertEquals(TaskRunStatus.PAUSED, paused.getTaskState().getStatus());
        assertEquals(TaskStage.EXECUTION, resumed.getTaskState().getStage());
        assertEquals(TaskRunStatus.RUNNING, resumed.getTaskState().getStatus());
    }

    @Test
    public void userIntentParserRecognizesConfirmationsWithoutTreatingFeedbackAsConfirmation() {
        assertTrue(TaskUserIntentParser.INSTANCE.isConfirmation("подтверждаю"));
        assertTrue(TaskUserIntentParser.INSTANCE.isConfirmation("да, подтверждаю"));
        assertTrue(TaskUserIntentParser.INSTANCE.isConfirmation("ок, продолжай"));
        assertTrue(TaskUserIntentParser.INSTANCE.isConfirmation("Идём дальше!"));
        assertFalse(TaskUserIntentParser.INSTANCE.isConfirmation("Удали Firebase"));
        assertFalse(TaskUserIntentParser.INSTANCE.isConfirmation("Добавь пункт про тестирование"));
    }

    @Test
    public void userIntentParserRecognizesRequestedStage() {
        assertEquals(
            TaskStage.VALIDATION,
            TaskUserIntentParser.INSTANCE.parseRequestedStage("перейди сразу к validation")
        );
        assertEquals(
            TaskStage.DONE,
            TaskUserIntentParser.INSTANCE.parseRequestedStage("давай сразу done")
        );
    }

    @Test
    public void userIntentParserSeparatesQuestionsAndEditRequests() {
        assertTrue(TaskUserIntentParser.INSTANCE.isClarifyingQuestion(
            "Почему ты добавил permissions?"
        ));
        assertFalse(TaskUserIntentParser.INSTANCE.isEditRequest(
            "Почему ты добавил permissions?"
        ));
        assertTrue(TaskUserIntentParser.INSTANCE.isEditRequest(
            "Добавь пункт про deep links."
        ));
        assertFalse(TaskUserIntentParser.INSTANCE.isClarifyingQuestion(
            "Добавь пункт про deep links."
        ));
        assertTrue(TaskUserIntentParser.INSTANCE.isEditRequest(
            "Добавь пояснение, почему нужны permissions?"
        ));
    }

    @Test
    public void feedbackUpdatesCurrentResultWithoutChangingStage() {
        TaskContext waiting = stateMachine.completeStage(
            task(TaskStage.PLANNING, TaskRunStatus.RUNNING),
            "initial plan"
        );
        TaskContext running = stateMachine.beginFeedback(waiting);
        TaskContext updated = stateMachine.completeFeedback(running, "updated plan");

        assertEquals(TaskStage.PLANNING, updated.getTaskState().getStage());
        assertEquals(TaskRunStatus.WAITING_USER, updated.getTaskState().getStatus());
        assertEquals("updated plan", updated.getPlanningResult());
    }

    @Test
    public void planningCompletionPreservesSwarmResultsAndWaitsForUser() {
        PlanningSwarmResult requirements = new PlanningSwarmResult(
            PlanningSwarmRole.REQUIREMENTS,
            "RequirementsPlanningAgent",
            "requirements",
            System.currentTimeMillis()
        );
        TaskContext withSwarm = task(TaskStage.PLANNING, TaskRunStatus.RUNNING).copy(
            "id",
            "title",
            "request",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "",
            new TaskState(TaskStage.PLANNING, TaskRunStatus.RUNNING, ""),
            "",
            "",
            "",
            Collections.singletonList(requirements),
            System.currentTimeMillis()
        );

        TaskContext planned = stateMachine.completeStage(withSwarm, "final plan");

        assertEquals(1, planned.getPlanningSwarmResults().size());
        assertEquals(requirements, planned.getPlanningSwarmResults().get(0));
        assertEquals("final plan", planned.getPlanningResult());
        assertEquals(TaskRunStatus.WAITING_USER, planned.getTaskState().getStatus());
    }

    @Test
    public void validationReportValidatorRejectsMissingSectionsAndCopiedExecution() {
        String execution = String.join(
            "",
            Collections.nCopies(20, "Техническое задание ")
        );
        String validReport =
            "# Validation Report\n\n" +
            "## Статус\nOK_WITH_RECOMMENDATIONS\n\n" +
            "## Что выполнено хорошо\n- Архитектура описана.\n\n" +
            "## Замечания\n- Не описан отказ в разрешении.\n\n" +
            "## Риски\n- Возможны ограничения доставки.\n\n" +
            "## Рекомендации\n1. Добавить edge cases.\n\n" +
            "## Итог\nНужны небольшие уточнения.";

        assertTrue(ValidationReportValidator.INSTANCE.isValid(validReport, execution));
        assertFalse(ValidationReportValidator.INSTANCE.isValid("Всё хорошо", execution));
        assertTrue(
            ValidationReportValidator.INSTANCE.looksLikeCopiedExecutionResult(
                validReport + "\n" + execution.substring(0, 300),
                execution
            )
        );
    }

    @Test
    public void revalidationKeepsUpdatedExecutionAndWaitsBeforeDone() {
        TaskContext validationRunning = new TaskContext(
            "id",
            "title",
            "request",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "",
            new TaskState(
                TaskStage.VALIDATION,
                TaskRunStatus.RUNNING,
                "Validation agent is rechecking updated execution result"
            ),
            "plan",
            "updated execution with analytics",
            "",
            Collections.emptyList(),
            System.currentTimeMillis()
        );

        TaskContext revalidated = stateMachine.completeStage(
            validationRunning,
            "# Validation Report"
        );

        assertEquals(
            "updated execution with analytics",
            revalidated.getExecutionResult()
        );
        assertEquals("# Validation Report", revalidated.getValidationResult());
        assertEquals(TaskStage.VALIDATION, revalidated.getTaskState().getStage());
        assertEquals(
            TaskRunStatus.WAITING_USER,
            revalidated.getTaskState().getStatus()
        );
    }

    private TaskContext task(TaskStage stage, TaskRunStatus status) {
        return new TaskContext(
            "id",
            "title",
            "request",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "",
            new TaskState(stage, status, ""),
            "",
            "",
            "",
            Collections.emptyList(),
            System.currentTimeMillis()
        );
    }
}
