package com.sage.teachingassistant.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.teachingassistant.domain.ExecutionStatus;
import com.sage.teachingassistant.domain.RunStatus;
import com.sage.teachingassistant.domain.StageExecution;
import com.sage.teachingassistant.domain.WorkflowRun;
import com.sage.teachingassistant.repository.StageExecutionRepository;
import com.sage.teachingassistant.repository.WorkflowRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises the engine's branching without a database.
 *
 * <p>The repositories are backed by in-memory collections rather than stubbed
 * method by method, so the run genuinely accumulates state across calls — which is
 * the behaviour under test.
 */
class WorkflowEngineTest {

    private static final String WORKFLOW = "test-workflow";

    private final Map<String, WorkflowRun> runs = new HashMap<>();
    private final List<StageExecution> executions = new ArrayList<>();

    private StubStage confirm;
    private StubStage research;
    private StubStage template;
    private WorkflowEngine engine;

    @BeforeEach
    void setUp() {
        confirm = new StubStage(WORKFLOW, "confirm", "Confirm");
        research = new StubStage(WORKFLOW, "research", "Research");
        template = new StubStage(WORKFLOW, "template", "Template");

        WorkflowRegistry registry = new WorkflowRegistry(List.of(
                new WorkflowDefinition(WORKFLOW, "Test workflow", List.of(confirm, research, template))));

        WorkflowRunRepository runRepository = mock(WorkflowRunRepository.class);
        StageExecutionRepository executionRepository = mock(StageExecutionRepository.class);

        when(runRepository.save(any(WorkflowRun.class))).thenAnswer(invocation -> {
            WorkflowRun run = invocation.getArgument(0);
            runs.put(run.getId(), run);
            return run;
        });
        when(runRepository.findById(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(runs.get(invocation.getArgument(0))));

        when(executionRepository.save(any(StageExecution.class))).thenAnswer(invocation -> {
            StageExecution execution = invocation.getArgument(0);
            if (executions.stream().noneMatch(existing -> existing == execution)) {
                executions.add(execution);
            }
            return execution;
        });
        when(executionRepository.findByRunIdOrderByCreatedAtAsc(anyString())).thenAnswer(invocation -> {
            String runId = invocation.getArgument(0);
            return executions.stream().filter(e -> e.getRunId().equals(runId)).toList();
        });
        when(executionRepository.countByRunIdAndStageKey(anyString(), anyString())).thenAnswer(invocation -> {
            String runId = invocation.getArgument(0);
            String stageKey = invocation.getArgument(1);
            return executions.stream()
                    .filter(e -> e.getRunId().equals(runId) && e.getStageKey().equals(stageKey))
                    .count();
        });
        when(executionRepository.findFirstByRunIdAndStageKeyOrderByAttemptDesc(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String runId = invocation.getArgument(0);
                    String stageKey = invocation.getArgument(1);
                    return executions.stream()
                            .filter(e -> e.getRunId().equals(runId) && e.getStageKey().equals(stageKey))
                            .max(Comparator.comparingInt(StageExecution::getAttempt));
                });

        engine = new WorkflowEngine(registry, runRepository, executionRepository,
                new KeywordApprovalClassifier(), new ObjectMapper());
    }

    // ------------------------------------------------------------------
    // starting
    // ------------------------------------------------------------------

    @Test
    void startRunsOnlyTheFirstStage() {
        WorkflowTurn turn = engine.start(WORKFLOW, "notes on photosynthesis");

        assertThat(turn.stageIndex()).isZero();
        assertThat(turn.stageKey()).isEqualTo("confirm");
        assertThat(turn.stageCount()).isEqualTo(3);
        assertThat(turn.status()).isEqualTo(RunStatus.AWAITING_APPROVAL);
        assertThat(turn.isCompleted()).isFalse();

        assertThat(confirm.received).hasSize(1);
        assertThat(confirm.lastContext().userMessage()).isEqualTo("notes on photosynthesis");
        assertThat(confirm.lastContext().attempt()).isEqualTo(1);
        assertThat(research.received).isEmpty();
        assertThat(template.received).isEmpty();
    }

    @Test
    void startRejectsAnUnknownWorkflow() {
        assertThatThrownBy(() -> engine.start("nope", "hello"))
                .isInstanceOf(WorkflowNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // the approval gate
    // ------------------------------------------------------------------

    @Test
    void approvalAdvancesToTheNextStage() {
        WorkflowTurn started = engine.start(WORKFLOW, "a request");
        WorkflowTurn after = engine.handleMessage(started.runId(), "yes");

        assertThat(after.stageIndex()).isEqualTo(1);
        assertThat(after.stageKey()).isEqualTo("research");
        assertThat(research.received).hasSize(1);
        assertThat(template.received).isEmpty();
    }

    @Test
    void nonApprovalStaysOnTheSameStageAndBecomesFeedback() {
        WorkflowTurn started = engine.start(WORKFLOW, "a request");
        WorkflowTurn after = engine.handleMessage(started.runId(), "make it about respiration");

        assertThat(after.stageIndex()).isZero();
        assertThat(after.stageKey()).isEqualTo("confirm");

        assertThat(confirm.received).hasSize(2);
        assertThat(confirm.lastContext().isRevision()).isTrue();
        assertThat(confirm.lastContext().feedback()).isEqualTo("make it about respiration");
        assertThat(confirm.lastContext().attempt()).isEqualTo(2);

        // Nothing downstream may run until the user approves.
        assertThat(research.received).isEmpty();
    }

    @Test
    void approvalWithTrailingContentIsTreatedAsFeedback() {
        WorkflowTurn started = engine.start(WORKFLOW, "a request");
        WorkflowTurn after = engine.handleMessage(started.runId(), "yes but shorten the intro");

        assertThat(after.stageIndex()).isZero();
        assertThat(confirm.received).hasSize(2);
    }

    @Test
    void approvingTheLastStageCompletesTheRun() {
        WorkflowTurn turn = engine.start(WORKFLOW, "a request");
        turn = engine.handleMessage(turn.runId(), "yes");
        turn = engine.handleMessage(turn.runId(), "yes");
        turn = engine.handleMessage(turn.runId(), "yes");

        assertThat(turn.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(turn.isCompleted()).isTrue();
        assertThat(turn.stageKey()).isEqualTo("template");
        assertThat(runs.get(turn.runId()).getCompletedAt()).isNotNull();
    }

    @Test
    void aFinishedRunRejectsFurtherMessages() {
        WorkflowTurn turn = engine.start(WORKFLOW, "a request");
        turn = engine.handleMessage(turn.runId(), "yes");
        turn = engine.handleMessage(turn.runId(), "yes");
        turn = engine.handleMessage(turn.runId(), "yes");

        String runId = turn.runId();
        assertThatThrownBy(() -> engine.handleMessage(runId, "yes"))
                .isInstanceOf(RunFinishedException.class);
    }

    @Test
    void anUnknownRunIsRejected() {
        assertThatThrownBy(() -> engine.handleMessage("no-such-run", "yes"))
                .isInstanceOf(RunNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // passing data forward
    // ------------------------------------------------------------------

    @Test
    void theNextStageSeesTheApprovedOutputOfEarlierStages() {
        WorkflowTurn turn = engine.start(WORKFLOW, "a request");
        engine.handleMessage(turn.runId(), "yes");

        assertThat(research.lastContext().priorOutputs()).containsKey("confirm.attempt");
        assertThat(research.lastContext().priorOutput("confirm.attempt")).isEqualTo("1");
    }

    @Test
    void anApprovalCarriesNoContentIntoTheNextStage() {
        WorkflowTurn turn = engine.start(WORKFLOW, "a request");
        engine.handleMessage(turn.runId(), "yes");

        // "yes" is not content; the next stage works from priorOutputs instead.
        assertThat(research.lastContext().userMessage()).isNull();
        assertThat(research.lastContext().feedback()).isNull();
        assertThat(research.lastContext().isRevision()).isFalse();
    }

    @Test
    void aRevisedStageSupersedesItsEarlierOutput() {
        WorkflowTurn turn = engine.start(WORKFLOW, "a request");
        turn = engine.handleMessage(turn.runId(), "change it");   // confirm attempt 2
        engine.handleMessage(turn.runId(), "yes");                 // -> research

        // research must see attempt 2, not the superseded attempt 1
        assertThat(research.lastContext().priorOutput("confirm.attempt")).isEqualTo("2");
    }

    @Test
    void aStageIsNotOfferedItsOwnEarlierOutput() {
        WorkflowTurn turn = engine.start(WORKFLOW, "a request");
        engine.handleMessage(turn.runId(), "change it");

        // confirm re-running must not see its own previous output
        assertThat(confirm.lastContext().priorOutputs()).isEmpty();
    }

    // ------------------------------------------------------------------
    // history
    // ------------------------------------------------------------------

    @Test
    void historyRecordsEveryAttemptAndItsOutcome() {
        WorkflowTurn turn = engine.start(WORKFLOW, "a request");
        turn = engine.handleMessage(turn.runId(), "change it");
        turn = engine.handleMessage(turn.runId(), "yes");

        RunDetail detail = engine.describe(turn.runId());

        assertThat(detail.history()).hasSize(3);
        assertThat(detail.history().get(0).status()).isEqualTo(ExecutionStatus.REVISED);
        assertThat(detail.history().get(0).attempt()).isEqualTo(1);
        assertThat(detail.history().get(1).status()).isEqualTo(ExecutionStatus.APPROVED);
        assertThat(detail.history().get(1).attempt()).isEqualTo(2);
        assertThat(detail.history().get(1).feedback()).isEqualTo("change it");
        assertThat(detail.history().get(2).status()).isEqualTo(ExecutionStatus.EXECUTED);
        assertThat(detail.history().get(2).stageKey()).isEqualTo("research");
    }

    @Test
    void describeRejectsAnUnknownRun() {
        assertThatThrownBy(() -> engine.describe("no-such-run"))
                .isInstanceOf(RunNotFoundException.class);
    }

    @Test
    void aRunKeepsItsOwnPositionIndependentOfOthers() {
        WorkflowTurn first = engine.start(WORKFLOW, "first request");
        WorkflowTurn second = engine.start(WORKFLOW, "second request");

        engine.handleMessage(first.runId(), "yes");

        assertThat(engine.describe(first.runId()).stageIndex()).isEqualTo(1);
        assertThat(engine.describe(second.runId()).stageIndex()).isZero();
    }
}
