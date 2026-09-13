package com.sage.teachingassistant.workflow;

import com.sage.teachingassistant.domain.RunStatus;

import java.util.Map;

/**
 * The result of one interaction with the engine — either the run just started, or
 * the engine just reacted to a message.
 *
 * @param runId       the run to send the next message to
 * @param status      where the run now stands
 * @param stageIndex  zero-based position of the stage this response describes
 * @param stageCount  how many stages the workflow has
 * @param stageKey    identifier of that stage
 * @param stageName   readable name of that stage
 * @param message     what to show the user
 * @param output      what the stage produced
 */
public record WorkflowTurn(
        String runId,
        String workflowKey,
        RunStatus status,
        int stageIndex,
        int stageCount,
        String stageKey,
        String stageName,
        String message,
        Map<String, String> output
) {

    public boolean isCompleted() {
        return status == RunStatus.COMPLETED;
    }
}
