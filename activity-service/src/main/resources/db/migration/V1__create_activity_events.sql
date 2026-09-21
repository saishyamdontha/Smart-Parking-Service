CREATE TABLE activity_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    session_id BIGINT,
    space_id BIGINT,
    user_id VARCHAR(100),
    recorded_at TIMESTAMPTZ NOT NULL
);
