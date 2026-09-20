package com.sage.teachingassistant.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Payload sent from the mobile app when a user clicks the Feedback button
 * to register a suggestion or preference for a specific stage or globally.
 */
public record FeedbackRequest(

        @JsonAlias({"run_id", "runId", "run"})
        String runId,

        @JsonAlias({"stage_key", "stageKey", "stage", "step"})
        String stageKey,

        @JsonAlias({"workflow_key", "workflowKey", "workflow", "type"})
        String workflowKey,

        @JsonAlias({"suggestion", "feedback", "rule", "text", "comment", "instruction"})
        String suggestion,

        @JsonAlias({"category", "tag", "type"})
        String category
) {
    public String resolveStageKey() {
        return (stageKey != null && !stageKey.isBlank()) ? stageKey.trim() : "ALL";
    }

    public String resolveSuggestion() {
        return (suggestion != null && !suggestion.isBlank()) ? suggestion.trim() : "";
    }
}
