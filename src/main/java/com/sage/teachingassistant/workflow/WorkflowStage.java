package com.sage.teachingassistant.workflow;

/**
 * One fixed step of a workflow.
 *
 * <p>A stage does one job and that job never changes. It receives a
 * {@link StageContext}, does its work, and returns a {@link StageResult}. It must
 * not reach into the database for another stage's data, call another stage, or
 * assume anything about what runs before or after it beyond what arrives in the
 * context.
 *
 * <p>Implementations are Spring beans. Ordering is not declared here — the
 * sequence lives in {@link WorkflowDefinition}, so there is only one place that
 * says what runs when.
 */
public interface WorkflowStage {

    /** Which workflow this stage belongs to. */
    String workflowKey();

    /** Stable identifier, used in the API and in execution history. */
    String key();

    /** Human-readable name. */
    String name();

    StageResult execute(StageContext context);
}
