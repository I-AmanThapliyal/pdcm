package com.pdcm.traffic;

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
public class TrafficStatusService {

	private final DestinationRepository destinationRepository;
	private final RecentObservations recentObservations;
	private final Clock clock;

	public TrafficStatusService(
			DestinationRepository destinationRepository,
			RecentObservations recentObservations,
			Clock clock) {
		this.destinationRepository = destinationRepository;
		this.recentObservations = recentObservations;
		this.clock = clock;
	}

	public TrafficSummary current(Long destinationId) {
		destinationRepository.findByIdAndActiveTrue(destinationId)
				.orElseThrow(() -> new NotFoundException("Destination not found: " + destinationId));
		Instant now = clock.instant();
		List<CrowdObservation> reports = recentObservations.since(destinationId, now);
		return TrafficAggregator.summarize(destinationId, reports, now);
	}
}
