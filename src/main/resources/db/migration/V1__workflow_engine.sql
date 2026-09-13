-- Sage :: workflow engine
--
-- A run walks a fixed sequence of stages. It only moves forward when the user
-- approves the current stage's output, so the run's position has to survive
-- between HTTP requests. That is what these two tables are for.
--
-- Hibernate runs with ddl-auto=validate, so every column type here must match
-- its entity mapping exactly.

CREATE TABLE workflow_runs
(
    id                  VARCHAR(36) PRIMARY KEY,
    workflow_key        VARCHAR(64)              NOT NULL,
    status              VARCHAR(32)              NOT NULL,
    current_stage_index INTEGER                  NOT NULL,
    created_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completed_at        TIMESTAMP(6) WITH TIME ZONE
);

COMMENT ON TABLE workflow_runs IS 'One traversal of a workflow by a user.';
COMMENT ON COLUMN workflow_runs.status IS 'AWAITING_APPROVAL, COMPLETED or CANCELLED.';

-- Every stage execution is kept, including revisions, so the full history of a
-- run can be replayed.
CREATE TABLE stage_executions
(
    id             BIGSERIAL PRIMARY KEY,
    run_id         VARCHAR(36)              NOT NULL
        REFERENCES workflow_runs (id) ON DELETE CASCADE,
    stage_key      VARCHAR(64)              NOT NULL,
    stage_index    INTEGER                  NOT NULL,
    attempt        INTEGER                  NOT NULL,
    status         VARCHAR(32)              NOT NULL,
    user_message   VARCHAR(4096),
    feedback       VARCHAR(4096),
    result_message VARCHAR(4096),
    output_payload VARCHAR(8192),
    created_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

COMMENT ON COLUMN stage_executions.attempt IS '1 for the first try, incrementing on each revision.';
COMMENT ON COLUMN stage_executions.output_payload IS 'JSON produced by the stage, handed to the next stage.';

CREATE INDEX idx_stage_executions_run ON stage_executions (run_id, created_at);
