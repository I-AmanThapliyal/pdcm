-- Phase 5: user destination suggestions + admin approval audit.
-- Suggestions stay separate from destinations until explicitly approved.
-- Approval never seeds visit_reports or fabricated traffic.

CREATE TABLE destination_suggestions (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(200)  NOT NULL,
    normalized_name         VARCHAR(200)  NOT NULL,
    city                    VARCHAR(120)  NOT NULL,
    region                  VARCHAR(100)  NOT NULL,
    category                VARCHAR(50)   NOT NULL,
    description             TEXT          NOT NULL,
    latitude                DOUBLE PRECISION,
    longitude               DOUBLE PRECISION,
    reference_url           VARCHAR(500),
    submitter_user_id       BIGINT        NOT NULL REFERENCES users (id),
    status                  VARCHAR(32)   NOT NULL DEFAULT 'SUBMITTED',
    admin_note              VARCHAR(500),
    created_destination_id  BIGINT        REFERENCES destinations (id),
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_destination_suggestions_status CHECK (
        status IN ('SUBMITTED', 'APPROVED', 'REJECTED')
    ),
    CONSTRAINT chk_destination_suggestions_category CHECK (
        category IN (
            'HISTORICAL',
            'TEMPLE',
            'BEACH',
            'HILL_STATION',
            'WILDLIFE',
            'CITY',
            'NATURE'
        )
    ),
    CONSTRAINT chk_destination_suggestions_latitude CHECK (
        latitude IS NULL OR latitude BETWEEN -90 AND 90
    ),
    CONSTRAINT chk_destination_suggestions_longitude CHECK (
        longitude IS NULL OR longitude BETWEEN -180 AND 180
    ),
    CONSTRAINT chk_destination_suggestions_coords_pair CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    ),
    CONSTRAINT chk_destination_suggestions_approved_link CHECK (
        (status = 'APPROVED' AND created_destination_id IS NOT NULL)
        OR (status <> 'APPROVED' AND created_destination_id IS NULL)
    )
);

CREATE INDEX idx_destination_suggestions_status_created
    ON destination_suggestions (status, created_at DESC);

CREATE INDEX idx_destination_suggestions_submitter_created
    ON destination_suggestions (submitter_user_id, created_at DESC);

CREATE INDEX idx_destination_suggestions_normalized_region
    ON destination_suggestions (normalized_name, region);

-- At most one catalogue destination may be produced from a suggestion.
CREATE UNIQUE INDEX uq_destination_suggestions_created_destination
    ON destination_suggestions (created_destination_id)
    WHERE created_destination_id IS NOT NULL;

CREATE TABLE destination_suggestion_events (
    id              BIGSERIAL PRIMARY KEY,
    suggestion_id   BIGINT       NOT NULL REFERENCES destination_suggestions (id),
    actor_user_id   BIGINT       NOT NULL REFERENCES users (id),
    action          VARCHAR(32)  NOT NULL,
    from_status     VARCHAR(32)  NOT NULL,
    to_status       VARCHAR(32)  NOT NULL,
    note            VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_suggestion_events_action CHECK (
        action IN ('SUBMITTED', 'APPROVED', 'REJECTED')
    ),
    CONSTRAINT chk_suggestion_events_from_status CHECK (
        from_status IN ('SUBMITTED', 'APPROVED', 'REJECTED')
    ),
    CONSTRAINT chk_suggestion_events_to_status CHECK (
        to_status IN ('SUBMITTED', 'APPROVED', 'REJECTED')
    )
);

CREATE INDEX idx_destination_suggestion_events_suggestion_created
    ON destination_suggestion_events (suggestion_id, created_at DESC);

CREATE INDEX idx_destination_suggestion_events_created
    ON destination_suggestion_events (created_at DESC);
