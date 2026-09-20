package com.sage.teachingassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Captures user/teacher feedback for a specific stage or global guidelines,
 * which is automatically synthesized into rules for subsequent LLM responses.
 */
@Entity
@Table(name = "improvement_rules")
public class ImprovementRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", length = 36)
    private String runId;

    @Column(name = "stage_key", length = 64, nullable = false)
    private String stageKey;

    @Column(name = "workflow_key", length = 64)
    private String workflowKey;

    @Column(name = "rule_text", columnDefinition = "TEXT", nullable = false)
    private String ruleText;

    @Column(name = "category", length = 32)
    private String category;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ImprovementRule() {
        // for JPA
    }

    public ImprovementRule(String runId, String stageKey, String workflowKey, String ruleText, String category) {
        this.runId = runId;
        this.stageKey = (stageKey != null && !stageKey.isBlank()) ? stageKey.trim() : "ALL";
        this.workflowKey = workflowKey;
        this.ruleText = ruleText != null ? ruleText.trim() : "";
        this.category = (category != null && !category.isBlank()) ? category.trim().toUpperCase() : "GENERAL";
        this.isActive = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public ImprovementRule(String stageKey, String ruleText) {
        this(null, stageKey, null, ruleText, "GENERAL");
    }

    public Long getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public String getStageKey() {
        return stageKey;
    }

    public String getWorkflowKey() {
        return workflowKey;
    }

    public String getRuleText() {
        return ruleText;
    }

    public String getCategory() {
        return category;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateRuleText(String newRuleText) {
        this.ruleText = newRuleText != null ? newRuleText.trim() : "";
        this.updatedAt = Instant.now();
    }

    public void setActive(boolean active) {
        this.isActive = active;
        this.updatedAt = Instant.now();
    }
}
