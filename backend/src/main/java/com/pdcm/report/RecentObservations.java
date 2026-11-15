package com.pdcm.report;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecentObservations {

	private final VisitReportRepository visitReportRepository;

	public RecentObservations(VisitReportRepository visitReportRepository) {
		this.visitReportRepository = visitReportRepository;
	}

	public List<CrowdObservation> since(Long destinationId, Instant now) {
		Instant cutoff = now.minus(ReportWindow.FRESHNESS);
		return visitReportRepository.findFreshByDestination(destinationId, cutoff).stream()
				.map(RecentObservations::toObservation)
				.toList();
	}

	/**
	 * One query for every destination's in-window LOCATION_VERIFIED reports.
	 * Destinations with no fresh reports are absent from the map (treat as empty).
	 */
	public Map<Long, List<CrowdObservation>> allFresh(Instant now) {
		Instant cutoff = now.minus(ReportWindow.FRESHNESS);
		Map<Long, List<CrowdObservation>> byDestination = new HashMap<>();
		for (VisitReport report : visitReportRepository.findAllFresh(cutoff)) {
			byDestination
					.computeIfAbsent(report.getDestination().getId(), ignored -> new ArrayList<>())
					.add(toObservation(report));
		}
		return byDestination;
	}

	private static CrowdObservation toObservation(VisitReport report) {
		return new CrowdObservation(
				report.getUser().getId(),
				report.getCrowdIntensity(),
				report.getTrafficCondition(),
				report.getObservedAt());
	}
}
