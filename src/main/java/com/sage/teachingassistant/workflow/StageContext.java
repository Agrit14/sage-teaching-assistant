package com.sage.teachingassistant.workflow;

import java.util.Map;

/**
 * Everything a stage is given when it runs.
 *
 * <p>A stage never talks to another stage directly. It reads what it needs from
 * here and returns a {@link StageResult}, which is the only thing that travels
 * forward. That is what keeps the stages independent.
 *
 * @param runId        the run this execution belongs to
 * @param workflowKey  which workflow is running
 * @param userMessage  the message that triggered this execution, or {@code null}
 *                     when the stage is running because the previous one was approved
 * @param feedback     what the user asked to change; {@code null} unless this is a re-run
 * @param attempt      1 on the first run of this stage, incrementing per revision
 * @param priorOutputs merged output of every stage already approved before this one
 */
public record StageContext(
        String runId,
        String workflowKey,
        String userMessage,
        String feedback,
        int attempt,
        Map<String, String> priorOutputs
) {

    /** True when this execution exists because the user rejected the previous one. */
    public boolean isRevision() {
        return feedback != null && !feedback.isBlank();
    }

    /** Convenience for stages that only care about the latest thing the user said. */
    public String latestUserInput() {
        return isRevision() ? feedback : userMessage;
    }

    /** Reads a value handed over by an earlier stage, or {@code null}. */
    public String priorOutput(String key) {
        return priorOutputs.get(key);
    }
}
