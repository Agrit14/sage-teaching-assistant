package com.sage.teachingassistant.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.teachingassistant.domain.StageExecution;
import com.sage.teachingassistant.domain.WorkflowRun;
import com.sage.teachingassistant.repository.StageExecutionRepository;
import com.sage.teachingassistant.repository.WorkflowRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Drives runs forward.
 *
 * <p>A run moves on only when the user approves the current stage. Approving marks
 * that execution approved and starts the next stage; anything else re-runs the
 * same stage with the user's message as feedback.
 *
 * <p>Deliberately not annotated {@code @Transactional}. Each repository call is
 * its own short transaction, which means a stage runs with no transaction open.
 * That matters: once these stages do real work — a web search, a model call, a
 * PDF render — holding a database connection across them would exhaust the pool.
 */
@Service
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private static final String COMPLETION_MESSAGE =
            "Approved. That was the final stage, so this run is complete.";

    private final WorkflowRegistry registry;
    private final WorkflowRunRepository runRepository;
    private final StageExecutionRepository executionRepository;
    private final ApprovalClassifier approvalClassifier;
    private final ObjectMapper objectMapper;

    public WorkflowEngine(WorkflowRegistry registry,
                          WorkflowRunRepository runRepository,
                          StageExecutionRepository executionRepository,
                          ApprovalClassifier approvalClassifier,
                          ObjectMapper objectMapper) {
        this.registry = registry;
        this.runRepository = runRepository;
        this.executionRepository = executionRepository;
        this.approvalClassifier = approvalClassifier;
        this.objectMapper = objectMapper;
    }

    /** Creates a run and executes its first stage. */
    public WorkflowTurn start(String workflowKey, String message) {
        WorkflowDefinition definition = registry.require(workflowKey);
        WorkflowRun run = runRepository.save(new WorkflowRun(workflowKey));
        log.info("Run {} started on workflow '{}'", run.getId(), workflowKey);

        return runStage(run, definition, 0, message, null);
    }

    /**
     * Reacts to something the user said: either they approved the current stage,
     * or they are asking for it to change.
     */
    public WorkflowTurn handleMessage(String runId, String message) {
        WorkflowRun run = runRepository.findById(runId)
                .orElseThrow(() -> new RunNotFoundException(runId));

        if (run.isFinished()) {
            throw new RunFinishedException(runId, run.getStatus());
        }

        WorkflowDefinition definition = registry.require(run.getWorkflowKey());
        int index = run.getCurrentStageIndex();
        WorkflowStage stage = definition.stageAt(index);

        if (approvalClassifier.isApproval(message)) {
            return approve(run, definition, index, stage);
        }

        log.info("Run {} re-running stage '{}' on user feedback", runId, stage.key());
        return runStage(run, definition, index, message, message);
    }

    public RunDetail describe(String runId) {
        WorkflowRun run = runRepository.findById(runId)
                .orElseThrow(() -> new RunNotFoundException(runId));
        WorkflowDefinition definition = registry.require(run.getWorkflowKey());

        List<StageExecution> executions = executionRepository.findByRunIdOrderByCreatedAtAsc(runId);
        StageExecution latest = executions.isEmpty() ? null : executions.get(executions.size() - 1);
        int index = run.getCurrentStageIndex();
        WorkflowStage stage = definition.stageAt(index);

        return new RunDetail(
                run.getId(),
                run.getWorkflowKey(),
                run.getStatus(),
                index,
                definition.stageCount(),
                stage.key(),
                stage.name(),
                latest == null ? null : latest.getResultMessage(),
                latest == null ? Map.of() : readPayload(latest.getOutputPayload()),
                executions.stream().map(WorkflowEngine::toHistory).toList());
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    private WorkflowTurn approve(WorkflowRun run, WorkflowDefinition definition,
                                 int index, WorkflowStage stage) {

        executionRepository.findFirstByRunIdAndStageKeyOrderByAttemptDesc(run.getId(), stage.key())
                .ifPresent(execution -> {
                    execution.approve();
                    executionRepository.save(execution);
                });

        Optional<WorkflowStage> next = definition.stageAfter(index);
        if (next.isEmpty()) {
            run.complete();
            runRepository.save(run);
            log.info("Run {} completed", run.getId());
            return completedTurn(run, definition, index, stage);
        }

        run.moveToStage(index + 1);
        runRepository.save(run);
        log.info("Run {} advanced to stage '{}'", run.getId(), next.get().key());

        // The approval itself carries no content, so the next stage starts with no
        // user message. It works from priorOutputs instead.
        return runStage(run, definition, index + 1, null, null);
    }

    private WorkflowTurn runStage(WorkflowRun run, WorkflowDefinition definition,
                                  int index, String userMessage, String feedback) {

        WorkflowStage stage = definition.stageAt(index);
        int attempt = (int) executionRepository.countByRunIdAndStageKey(run.getId(), stage.key()) + 1;

        StageExecution execution = new StageExecution(run.getId(), stage.key(), index, attempt);
        execution.setUserMessage(userMessage);
        execution.setFeedback(feedback);

        StageContext context = new StageContext(
                run.getId(),
                run.getWorkflowKey(),
                userMessage,
                feedback,
                attempt,
                priorOutputs(run.getId(), index));

        // Runs with no transaction open, on purpose.
        StageResult result = stage.execute(context);

        execution.recordOutcome(result.message(), writePayload(result.output()));
        executionRepository.save(execution);

        run.touch();
        runRepository.save(run);

        return new WorkflowTurn(
                run.getId(),
                run.getWorkflowKey(),
                run.getStatus(),
                index,
                definition.stageCount(),
                stage.key(),
                stage.name(),
                result.message(),
                result.output());
    }

    private WorkflowTurn completedTurn(WorkflowRun run, WorkflowDefinition definition,
                                       int index, WorkflowStage stage) {
        Map<String, String> finalOutput = executionRepository
                .findFirstByRunIdAndStageKeyOrderByAttemptDesc(run.getId(), stage.key())
                .map(execution -> readPayload(execution.getOutputPayload()))
                .orElseGet(Map::of);

        return new WorkflowTurn(
                run.getId(),
                run.getWorkflowKey(),
                run.getStatus(),
                index,
                definition.stageCount(),
                stage.key(),
                stage.name(),
                COMPLETION_MESSAGE,
                finalOutput);
    }

    /**
     * Merges the output of every stage already approved before {@code uptoIndex}.
     * Later executions overwrite earlier ones, so a revised stage contributes its
     * final version only.
     */
    private Map<String, String> priorOutputs(String runId, int uptoIndex) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (StageExecution execution : executionRepository.findByRunIdOrderByCreatedAtAsc(runId)) {
            if (execution.getStageIndex() < uptoIndex && execution.getOutputPayload() != null) {
                merged.putAll(readPayload(execution.getOutputPayload()));
            }
        }
        return merged;
    }

    private String writePayload(Map<String, String> output) {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise stage output", e);
        }
    }

    private Map<String, String> readPayload(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("Stored stage output is not readable, treating as empty: {}", e.getMessage());
            return Map.of();
        }
    }

    private static RunDetail.Execution toHistory(StageExecution execution) {
        return new RunDetail.Execution(
                execution.getStageKey(),
                execution.getStageIndex(),
                execution.getAttempt(),
                execution.getStatus(),
                execution.getUserMessage(),
                execution.getFeedback(),
                execution.getResultMessage(),
                execution.getCreatedAt());
    }
}
