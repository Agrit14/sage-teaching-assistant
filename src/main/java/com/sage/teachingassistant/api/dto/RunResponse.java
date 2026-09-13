package com.sage.teachingassistant.api.dto;

import com.sage.teachingassistant.domain.RunStatus;
import com.sage.teachingassistant.workflow.WorkflowTurn;

import java.util.Map;

/** What the caller gets back after starting a run or sending it a message. */
public record RunResponse(
        String runId,
        String workflowKey,
        RunStatus status,
        int stageIndex,
        int stageCount,
        String stageKey,
        String stageName,
        String message,
        Map<String, String> output,
        boolean completed
) {

    public static RunResponse from(WorkflowTurn turn) {
        return new RunResponse(
                turn.runId(),
                turn.workflowKey(),
                turn.status(),
                turn.stageIndex(),
                turn.stageCount(),
                turn.stageKey(),
                turn.stageName(),
                turn.message(),
                turn.output(),
                turn.isCompleted());
    }
}
