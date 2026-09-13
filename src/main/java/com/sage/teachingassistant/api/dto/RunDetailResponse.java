package com.sage.teachingassistant.api.dto;

import com.sage.teachingassistant.domain.ExecutionStatus;
import com.sage.teachingassistant.domain.RunStatus;
import com.sage.teachingassistant.workflow.RunDetail;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** A run and everything that has happened to it. */
public record RunDetailResponse(
        String runId,
        String workflowKey,
        RunStatus status,
        int stageIndex,
        int stageCount,
        String stageKey,
        String stageName,
        String lastMessage,
        Map<String, String> output,
        List<HistoryEntry> history
) {

    public record HistoryEntry(
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

    public static RunDetailResponse from(RunDetail detail) {
        List<HistoryEntry> history = detail.history().stream()
                .map(entry -> new HistoryEntry(
                        entry.stageKey(),
                        entry.stageIndex(),
                        entry.attempt(),
                        entry.status(),
                        entry.userMessage(),
                        entry.feedback(),
                        entry.resultMessage(),
                        entry.at()))
                .toList();

        return new RunDetailResponse(
                detail.runId(),
                detail.workflowKey(),
                detail.status(),
                detail.stageIndex(),
                detail.stageCount(),
                detail.stageKey(),
                detail.stageName(),
                detail.lastMessage(),
                detail.output(),
                history);
    }
}
