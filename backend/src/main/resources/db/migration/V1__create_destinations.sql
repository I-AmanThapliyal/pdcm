CREATE TABLE destinations (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200)  NOT NULL,
    slug            VARCHAR(220)  NOT NULL,
    description     TEXT          NOT NULL,
    region          VARCHAR(100)  NOT NULL,
    category        VARCHAR(50)   NOT NULL,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    crowd_sensitivity VARCHAR(50),
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_destinations_slug UNIQUE (slug),
    CONSTRAINT chk_destinations_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_destinations_longitude CHECK (longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_destinations_region ON destinations (region);
CREATE INDEX idx_destinations_category ON destinations (category);
CREATE INDEX idx_destinations_active ON destinations (active);

CREATE TABLE destination_tags (
    destination_id  BIGINT       NOT NULL REFERENCES destinations (id) ON DELETE CASCADE,
    tag             VARCHAR(80)  NOT NULL,
    PRIMARY KEY (destination_id, tag)
);

CREATE INDEX idx_destination_tags_tag ON destination_tags (tag);
