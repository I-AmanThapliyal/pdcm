package com.pdcm.report;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges raw client latitude/longitude/accuracy after the configured retention
 * window. Never logs coordinate values. Idempotent and safe across restarts.
 */
@Service
public class LocationCoordinatePurgeService {

	private static final Logger log = LoggerFactory.getLogger(LocationCoordinatePurgeService.class);

	private final VisitReportRepository visitReportRepository;
	private final ReportVerificationProperties properties;
	private final Clock clock;

	public LocationCoordinatePurgeService(
			VisitReportRepository visitReportRepository,
			ReportVerificationProperties properties,
			Clock clock) {
		this.visitReportRepository = visitReportRepository;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public int purgeExpired() {
		Instant now = clock.instant();
		Instant cutoff = now.minus(properties.getCoordinateRetention());
		int updated = visitReportRepository.purgeClientCoordinatesOlderThan(cutoff, now);
		if (updated > 0) {
			log.info("Purged client coordinates from {} visit report(s) older than retention cutoff", updated);
		}
		return updated;
	}

	/**
	 * Defense-in-depth: if a single report is past retention, clear coords in-place.
	 * Does not log coordinate values.
	 */
	@Transactional
	public void purgeIfExpired(VisitReport report) {
		if (report == null || !report.hasRetainedClientCoordinates()) {
			return;
		}
		Instant cutoff = clock.instant().minus(properties.getCoordinateRetention());
		if (report.getCreatedAt() != null && report.getCreatedAt().isBefore(cutoff)) {
			report.purgeClientCoordinates(clock.instant());
		}
	}
}
