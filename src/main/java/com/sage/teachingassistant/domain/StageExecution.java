package com.sage.teachingassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The record of one stage running once. Revisions produce additional rows, so a
 * run's whole history is preserved rather than overwritten.
 */
@Entity
@Table(name = "stage_executions")
public class StageExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", length = 36, nullable = false)
    private String runId;

    @Column(name = "stage_key", length = 64, nullable = false)
    private String stageKey;

    @Column(name = "stage_index", nullable = false)
    private Integer stageIndex;

    /** 1 on the first run of this stage, incrementing on each revision. */
    @Column(name = "attempt", nullable = false)
    private Integer attempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private ExecutionStatus status;

    @Column(name = "user_message", length = 4096)
    private String userMessage;

    /** What the user asked to change, when this execution was a revision. */
    @Column(name = "feedback", length = 4096)
    private String feedback;

    @Column(name = "result_message", length = 4096)
    private String resultMessage;

    /** JSON the stage produced, handed on to the next stage. */
    @Column(name = "output_payload", length = 8192)
    private String outputPayload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StageExecution() {
        // for JPA
    }

    public StageExecution(String runId, String stageKey, int stageIndex, int attempt) {
        this.runId = runId;
        this.stageKey = stageKey;
        this.stageIndex = stageIndex;
        this.attempt = attempt;
        this.status = ExecutionStatus.EXECUTED;
        this.createdAt = Instant.now();
    }

    public void approve() {
        this.status = ExecutionStatus.APPROVED;
    }

    public void markRevised() {
        this.status = ExecutionStatus.REVISED;
    }

    public void recordOutcome(String resultMessage, String outputPayload) {
        this.resultMessage = resultMessage;
        this.outputPayload = outputPayload;
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

    public Integer getStageIndex() {
        return stageIndex;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public String getOutputPayload() {
        return outputPayload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
