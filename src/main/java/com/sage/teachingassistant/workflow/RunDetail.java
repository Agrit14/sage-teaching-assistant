package com.sage.teachingassistant.workflow;

import com.sage.teachingassistant.domain.ExecutionStatus;
import com.sage.teachingassistant.domain.RunStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** A full view of one run, including everything that has happened to it. */
public record RunDetail(
        String runId,
        String workflowKey,
        RunStatus status,
        int stageIndex,
        int stageCount,
        String stageKey,
        String stageName,
        String lastMessage,
        Map<String, String> output,
        List<Execution> history
) {

    /** One line of the run's audit trail. */
    public record Execution(
            String stageKey,
            int stageIndex,
            int attempt,
            ExecutionStatus status,
            String userMessage,
            String feedback,
            String resultMessage,
            Instant at
    ) {
    }
}
