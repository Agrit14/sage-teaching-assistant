-- Sage :: improvement layer
--
-- Stores teacher feedback and suggestions per stage or globally,
-- which are synthesized into prompt rules for the LLM.

CREATE TABLE improvement_rules
(
    id           BIGSERIAL PRIMARY KEY,
    run_id       VARCHAR(36),
    stage_key    VARCHAR(64)              NOT NULL,
    workflow_key VARCHAR(64),
    rule_text    TEXT                     NOT NULL,
    category     VARCHAR(32)              DEFAULT 'GENERAL',
    is_active    BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

COMMENT ON TABLE improvement_rules IS 'User/teacher feedback stored as rules to guide LLM responses.';
COMMENT ON COLUMN improvement_rules.stage_key IS 'Specific stage key or ALL for global rules.';

CREATE INDEX idx_improvement_rules_lookup ON improvement_rules (stage_key, is_active);
