package com.pdcm.recommendation;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdcm.common.Confidence;
import com.pdcm.common.GeoUtils;
import com.pdcm.common.NotFoundException;
import com.pdcm.crowd.CrowdStatusService;
import com.pdcm.crowd.CrowdSummary;
import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationRepository;
import com.pdcm.destination.DestinationResponse;
import com.pdcm.destination.DestinationService;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

	private final DestinationRepository destinationRepository;
	private final CrowdStatusService crowdStatusService;

	public RecommendationService(
			DestinationRepository destinationRepository,
			CrowdStatusService crowdStatusService) {
		this.destinationRepository = destinationRepository;
		this.crowdStatusService = crowdStatusService;
	}

	public List<AlternativeResponse> alternatives(Long destinationId, double radiusKm, int limit) {
		if (radiusKm <= 0 || radiusKm > DestinationService.MAX_NEARBY_RADIUS_KM) {
			throw new IllegalArgumentException("radiusKm must be between 0 (exclusive) and "
					+ DestinationService.MAX_NEARBY_RADIUS_KM);
		}
		if (limit <= 0 || limit > 20) {
			throw new IllegalArgumentException("limit must be between 1 and 20");
		}
		Destination origin = destinationRepository.findByIdAndActiveTrue(destinationId)
				.orElseThrow(() -> new NotFoundException("Destination not found: " + destinationId));

		return destinationRepository.findActiveExcluding(destinationId).stream()
				.map(candidate -> toAlternative(origin, candidate))
				.filter(item -> item.distanceKm() <= radiusKm)
				.sorted(rank())
				.limit(limit)
				.toList();
	}

	private AlternativeResponse toAlternative(Destination origin, Destination candidate) {
		double distance = GeoUtils.haversineKm(
				origin.getLatitude(), origin.getLongitude(),
				candidate.getLatitude(), candidate.getLongitude());
		CrowdSummary crowd = crowdStatusService.current(candidate.getId());
		String quality = crowd.confidence() == Confidence.NONE ? "INSUFFICIENT_DATA" : "SUFFICIENT";
		return new AlternativeResponse(
				DestinationResponse.from(candidate),
				Math.round(distance * 10.0) / 10.0,
				origin.getCategory() == candidate.getCategory(),
				crowd.status(),
				crowd.score(),
				crowd.confidence(),
				quality,
				crowd.contributingReports(),
				crowd.lastObservedAt(),
				crowd.source());
	}

	private static Comparator<AlternativeResponse> rank() {
		return Comparator
				.comparing(AlternativeResponse::sameCategory).reversed()
				.thenComparing(item -> item.crowdConfidence() == Confidence.NONE)
				.thenComparing(item -> item.crowdScore() == null ? BigDecimal.valueOf(99) : item.crowdScore())
				.thenComparingDouble(AlternativeResponse::distanceKm);
	}
}
