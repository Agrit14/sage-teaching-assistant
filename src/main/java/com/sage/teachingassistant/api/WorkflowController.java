package com.sage.teachingassistant.api;

import com.sage.teachingassistant.api.dto.RunDetailResponse;
import com.sage.teachingassistant.api.dto.RunMessageRequest;
import com.sage.teachingassistant.api.dto.RunResponse;
import com.sage.teachingassistant.api.dto.StartRunRequest;
import com.sage.teachingassistant.api.dto.WorkflowSummaryResponse;
import com.sage.teachingassistant.workflow.WorkflowEngine;
import com.sage.teachingassistant.workflow.WorkflowRegistry;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The workflow API.
 *
 * <p>Three calls cover a whole run: start it, send it messages, and read it back.
 * Every message is either an approval, which advances the run, or feedback, which
 * re-runs the current stage.
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowEngine engine;
    private final WorkflowRegistry registry;

    public WorkflowController(WorkflowEngine engine, WorkflowRegistry registry) {
        this.engine = engine;
        this.registry = registry;
    }

    /** Lists the workflows available to start, with their stages in order. */
    @GetMapping
    public List<WorkflowSummaryResponse> list() {
        return registry.all().stream().map(WorkflowSummaryResponse::from).toList();
    }

    /** Starts a run and executes its first stage. */
    @PostMapping("/runs")
    public RunResponse start(@Valid @RequestBody StartRunRequest request) {
        return RunResponse.from(engine.start(request.workflowKey(), request.message()));
    }

    /**
     * Sends the run a message. An approval moves to the next stage; anything else
     * is treated as feedback and the current stage runs again.
     */
    @PostMapping("/runs/{runId}/messages")
    public RunResponse send(@PathVariable String runId,
                            @Valid @RequestBody RunMessageRequest request) {
        return RunResponse.from(engine.handleMessage(runId, request.message()));
    }

    /** The run's current position plus its full execution history. */
    @GetMapping("/runs/{runId}")
    public RunDetailResponse describe(@PathVariable String runId) {
        return RunDetailResponse.from(engine.describe(runId));
    }
}
