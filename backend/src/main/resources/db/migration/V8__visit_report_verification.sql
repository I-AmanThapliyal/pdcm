-- Phase 3: GPS proximity verification for community reports.
-- Precise client coordinates are private (never returned on public APIs).
-- Historical rows are preserved as LEGACY_UNVERIFIED (not deleted, not promoted).

ALTER TABLE visit_reports
    ADD COLUMN verification_status VARCHAR(32),
    ADD COLUMN verification_reason VARCHAR(40),
    ADD COLUMN client_latitude DOUBLE PRECISION,
    ADD COLUMN client_longitude DOUBLE PRECISION,
    ADD COLUMN client_accuracy_m DOUBLE PRECISION,
    ADD COLUMN distance_to_destination_m DOUBLE PRECISION,
    ADD COLUMN location_consent_at TIMESTAMPTZ,
    ADD COLUMN location_purged_at TIMESTAMPTZ;

UPDATE visit_reports
SET verification_status = 'LEGACY_UNVERIFIED',
    verification_reason = 'PRE_CUTOVER'
WHERE verification_status IS NULL;

ALTER TABLE visit_reports
    ALTER COLUMN verification_status SET NOT NULL,
    ALTER COLUMN verification_status SET DEFAULT 'PENDING';

ALTER TABLE visit_reports
    ADD CONSTRAINT chk_visit_reports_verification_status CHECK (
        verification_status IN (
            'LEGACY_UNVERIFIED',
            'PENDING',
            'LOCATION_VERIFIED',
            'FLAGGED',
            'REJECTED'
        )
    );

CREATE INDEX idx_visit_reports_dest_status_observed
    ON visit_reports (destination_id, verification_status, observed_at DESC);
