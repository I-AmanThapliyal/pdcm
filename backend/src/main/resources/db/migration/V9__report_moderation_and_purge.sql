-- Phase 4: admin moderation audit trail + coord purge support.
-- Precise client coordinates remain private; retention defaults to 24 hours (app config).

CREATE TABLE report_moderation_events (
    id              BIGSERIAL PRIMARY KEY,
    report_id       BIGINT       NOT NULL REFERENCES visit_reports (id),
    actor_user_id   BIGINT       NOT NULL REFERENCES users (id),
    from_status     VARCHAR(32)  NOT NULL,
    to_status       VARCHAR(32)  NOT NULL,
    note            VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_moderation_from_status CHECK (
        from_status IN (
            'LEGACY_UNVERIFIED',
            'PENDING',
            'LOCATION_VERIFIED',
            'FLAGGED',
            'REJECTED'
        )
    ),
    CONSTRAINT chk_moderation_to_status CHECK (
        to_status IN (
            'LEGACY_UNVERIFIED',
            'PENDING',
            'LOCATION_VERIFIED',
            'FLAGGED',
            'REJECTED'
        )
    )
);

CREATE INDEX idx_report_moderation_events_report_created
    ON report_moderation_events (report_id, created_at DESC);

CREATE INDEX idx_report_moderation_events_created
    ON report_moderation_events (created_at DESC);

-- Queue for admin review (FLAGGED first; PENDING may be listed separately).
CREATE INDEX idx_visit_reports_verification_status_created
    ON visit_reports (verification_status, created_at DESC);

-- Efficient purge of retained client coordinates past retention.
CREATE INDEX idx_visit_reports_coord_purge
    ON visit_reports (created_at)
    WHERE client_latitude IS NOT NULL
       OR client_longitude IS NOT NULL
       OR client_accuracy_m IS NOT NULL;

ALTER TABLE visit_reports
    DROP CONSTRAINT IF EXISTS chk_visit_reports_verification_reason;

ALTER TABLE visit_reports
    ADD CONSTRAINT chk_visit_reports_verification_reason CHECK (
        verification_reason IS NULL OR verification_reason IN (
            'PRE_CUTOVER',
            'NO_CONSENT',
            'LOCATION_UNAVAILABLE',
            'INVALID_COORDINATES',
            'SUSPICIOUS_COORDINATES',
            'ACCURACY_TOO_LOW',
            'OUTSIDE_GEOFENCE',
            'WITHIN_GEOFENCE',
            'ADMIN_VERIFIED',
            'ADMIN_REJECTED',
            'ADMIN_FLAGGED'
        )
    );
