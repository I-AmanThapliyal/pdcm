-- V7: provider health + daily request metadata only.
-- TomTom (and other) traffic payloads are NOT stored here.

CREATE TABLE provider_health (
    provider                VARCHAR(40)  PRIMARY KEY,
    status                  VARCHAR(20)  NOT NULL,
    last_success_at         TIMESTAMPTZ,
    last_failure_at         TIMESTAMPTZ,
    last_error_code         VARCHAR(80),
    consecutive_failures    INTEGER      NOT NULL DEFAULT 0,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_provider_health_status CHECK (status IN ('UP', 'DEGRADED', 'DOWN', 'DISABLED'))
);

CREATE TABLE provider_request_stats (
    id              BIGSERIAL PRIMARY KEY,
    provider        VARCHAR(40)  NOT NULL,
    day_utc         DATE         NOT NULL,
    request_count   INTEGER      NOT NULL DEFAULT 0,
    success_count   INTEGER      NOT NULL DEFAULT 0,
    failure_count   INTEGER      NOT NULL DEFAULT 0,
    budget_skipped  INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uq_provider_request_stats UNIQUE (provider, day_utc),
    CONSTRAINT chk_provider_request_stats_nonneg CHECK (
        request_count >= 0 AND success_count >= 0 AND failure_count >= 0 AND budget_skipped >= 0
    )
);

CREATE INDEX idx_provider_request_stats_day ON provider_request_stats (day_utc DESC);

INSERT INTO provider_health (provider, status, consecutive_failures, updated_at)
VALUES ('TOMTOM', 'DISABLED', 0, NOW());
