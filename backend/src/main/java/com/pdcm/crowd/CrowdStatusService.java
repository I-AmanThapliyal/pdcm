package com.pdcm.crowd;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdcm.common.NotFoundException;
import com.pdcm.destination.DestinationRepository;
import com.pdcm.report.CrowdObservation;
import com.pdcm.report.RecentObservations;

@Service
@Transactional(readOnly = true)
public class CrowdStatusService {

	private final DestinationRepository destinationRepository;
	private final RecentObservations recentObservations;
	private final Clock clock;

	public CrowdStatusService(
			DestinationRepository destinationRepository,
			RecentObservations recentObservations,
			Clock clock) {
		this.destinationRepository = destinationRepository;
		this.recentObservations = recentObservations;
		this.clock = clock;
	}

	public CrowdSummary current(Long destinationId) {
		requireActive(destinationId);
		Instant now = clock.instant();
		List<CrowdObservation> reports = recentObservations.since(destinationId, now);
		return CrowdAggregator.summarize(destinationId, reports, now);
	}

	private void requireActive(Long destinationId) {
		destinationRepository.findByIdAndActiveTrue(destinationId)
				.orElseThrow(() -> new NotFoundException("Destination not found: " + destinationId));
	}
}
