-- Day 2 preflight: ensure lookup indexes exist (idempotent with IF NOT EXISTS).
CREATE INDEX IF NOT EXISTS idx_destinations_region ON destinations (region);
CREATE INDEX IF NOT EXISTS idx_destinations_category ON destinations (category);
CREATE INDEX IF NOT EXISTS idx_destinations_active ON destinations (active);
CREATE INDEX IF NOT EXISTS idx_destination_tags_tag ON destination_tags (tag);
