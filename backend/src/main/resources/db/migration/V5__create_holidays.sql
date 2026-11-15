CREATE TABLE holidays (
    id            BIGSERIAL PRIMARY KEY,
    holiday_date  DATE         NOT NULL,
    name          VARCHAR(200) NOT NULL,
    scope         VARCHAR(20)  NOT NULL DEFAULT 'NATIONAL',
    CONSTRAINT uq_holidays_date UNIQUE (holiday_date),
    CONSTRAINT chk_holidays_scope CHECK (scope = 'NATIONAL')
);

CREATE INDEX idx_visit_reports_destination_observed ON visit_reports (destination_id, observed_at);

-- Fixed-date national holidays only. Lunar and state holidays are not invented here.
INSERT INTO holidays (holiday_date, name, scope) VALUES
('2026-01-26', 'Republic Day', 'NATIONAL'),
('2026-08-15', 'Independence Day', 'NATIONAL'),
('2026-10-02', 'Gandhi Jayanti', 'NATIONAL'),
('2026-12-25', 'Christmas', 'NATIONAL'),
('2027-01-26', 'Republic Day', 'NATIONAL'),
('2027-08-15', 'Independence Day', 'NATIONAL'),
('2027-10-02', 'Gandhi Jayanti', 'NATIONAL'),
('2027-12-25', 'Christmas', 'NATIONAL');
