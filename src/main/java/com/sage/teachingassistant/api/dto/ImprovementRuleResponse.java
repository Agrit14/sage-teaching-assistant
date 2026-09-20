package com.sage.teachingassistant.api.dto;

import com.sage.teachingassistant.domain.ImprovementRule;

import java.time.Instant;

public record ImprovementRuleResponse(
        Long id,
        String runId,
        String stageKey,
        String workflowKey,
        String ruleText,
        String category,
        boolean active,
        Instant createdAt
) {
    public static ImprovementRuleResponse from(ImprovementRule rule) {
        return new ImprovementRuleResponse(
                rule.getId(),
                rule.getRunId(),
                rule.getStageKey(),
                rule.getWorkflowKey(),
                rule.getRuleText(),
                rule.getCategory(),
                rule.isActive(),
                rule.getCreatedAt());
    }
}
