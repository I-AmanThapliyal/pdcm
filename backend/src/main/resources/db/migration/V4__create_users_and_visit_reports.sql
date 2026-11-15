CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(320)  NOT NULL,
    display_name    VARCHAR(100)  NOT NULL,
    password_hash   VARCHAR(100)  NOT NULL,
    role            VARCHAR(20)   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_users_role ON users (role);

CREATE TABLE visit_reports (
    id                  BIGSERIAL PRIMARY KEY,
    destination_id      BIGINT       NOT NULL REFERENCES destinations (id),
    user_id             BIGINT       NOT NULL REFERENCES users (id),
    crowd_intensity     INTEGER      NOT NULL,
    traffic_condition   VARCHAR(20)  NOT NULL,
    wait_minutes        INTEGER,
    note                VARCHAR(500),
    observed_at         TIMESTAMPTZ  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_visit_reports_crowd CHECK (crowd_intensity BETWEEN 1 AND 5),
    CONSTRAINT chk_visit_reports_traffic CHECK (traffic_condition IN ('LIGHT', 'MODERATE', 'HEAVY', 'UNKNOWN')),
    CONSTRAINT chk_visit_reports_wait CHECK (wait_minutes IS NULL OR wait_minutes BETWEEN 0 AND 600)
);

CREATE INDEX idx_visit_reports_destination_created ON visit_reports (destination_id, created_at DESC);
CREATE INDEX idx_visit_reports_user_created ON visit_reports (user_id, created_at DESC);
CREATE INDEX idx_visit_reports_user_destination_created ON visit_reports (user_id, destination_id, created_at DESC);
