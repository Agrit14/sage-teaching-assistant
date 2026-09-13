-- Sage Teaching Assistant :: initial schema
--
-- Hibernate runs with ddl-auto=validate, so every column type here must match
-- the entity mappings exactly. Change entities and migrations together.

CREATE TABLE learners
(
    id               BIGSERIAL PRIMARY KEY,
    telegram_chat_id BIGINT                   NOT NULL UNIQUE,
    telegram_user_id BIGINT,
    username         VARCHAR(255),
    first_name       VARCHAR(255),
    last_name        VARCHAR(255),
    language_code    VARCHAR(16),
    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    last_seen_at     TIMESTAMP(6) WITH TIME ZONE
);

COMMENT ON TABLE learners IS 'One row per Telegram chat that has talked to Sage.';

CREATE TABLE messages
(
    id                  BIGSERIAL PRIMARY KEY,
    learner_id          BIGINT                   NOT NULL
        REFERENCES learners (id) ON DELETE CASCADE,
    telegram_message_id BIGINT,
    direction           VARCHAR(16)              NOT NULL,
    text                VARCHAR(4096),
    created_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now()
);

COMMENT ON COLUMN messages.direction IS 'INBOUND = from the learner, OUTBOUND = from Sage.';

CREATE INDEX idx_messages_learner_created ON messages (learner_id, created_at DESC);
