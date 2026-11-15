-- Phase 5: allow admin coordinate updates on destination suggestions.
-- Expands suggestion event action CHECK to include COORDINATES_UPDATED.

ALTER TABLE destination_suggestion_events
    DROP CONSTRAINT chk_suggestion_events_action;

ALTER TABLE destination_suggestion_events
    ADD CONSTRAINT chk_suggestion_events_action CHECK (
        action IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'COORDINATES_UPDATED')
    );
