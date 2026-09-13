package com.sage.teachingassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One traversal of a workflow by a user.
 *
 * <p>The run holds nothing but its position. All the substance lives in
 * {@link StageExecution} rows, so a stage can be re-run without disturbing the
 * ones that came before it.
 */
@Entity
@Table(name = "workflow_runs")
public class WorkflowRun {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "workflow_key", length = 64, nullable = false)
    private String workflowKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private RunStatus status;

    /** Zero-based position in the workflow's stage list. */
    @Column(name = "current_stage_index", nullable = false)
    private Integer currentStageIndex;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected WorkflowRun() {
        // for JPA
    }

    public WorkflowRun(String workflowKey) {
        this.id = UUID.randomUUID().toString();
        this.workflowKey = workflowKey;
        this.status = RunStatus.AWAITING_APPROVAL;
        this.currentStageIndex = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void moveToStage(int stageIndex) {
        this.currentStageIndex = stageIndex;
        this.status = RunStatus.AWAITING_APPROVAL;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = RunStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void cancel() {
        this.status = RunStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public boolean isFinished() {
        return status == RunStatus.COMPLETED || status == RunStatus.CANCELLED;
    }

    public String getId() {
        return id;
    }

    public String getWorkflowKey() {
        return workflowKey;
    }

    public RunStatus getStatus() {
        return status;
    }

    public Integer getCurrentStageIndex() {
        return currentStageIndex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
