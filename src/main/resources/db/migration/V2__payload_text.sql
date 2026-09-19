-- Sage :: workflow engine
-- Expand execution text and payload columns to TEXT to avoid truncation for rich outlines/drafts

ALTER TABLE stage_executions
    ALTER COLUMN output_payload TYPE TEXT,
    ALTER COLUMN result_message TYPE TEXT,
    ALTER COLUMN user_message TYPE TEXT,
    ALTER COLUMN feedback TYPE TEXT;
