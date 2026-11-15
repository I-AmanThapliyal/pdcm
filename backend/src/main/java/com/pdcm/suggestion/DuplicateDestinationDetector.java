package com.pdcm.suggestion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.pdcm.common.GeoUtils;
import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationRepository;

/**
 * Heuristic catalogue duplicate detection: normalized name + region, and/or
 * Haversine proximity when both sides have coordinates.
 */
@Component
public class DuplicateDestinationDetector {

	/** Soft proximity window for potential duplicates (kilometres). */
	public static final double PROXIMITY_KM = 2.0;

	private final DestinationRepository destinationRepository;

	public DuplicateDestinationDetector(DestinationRepository destinationRepository) {
		this.destinationRepository = destinationRepository;
	}

	public List<PossibleDuplicateResponse> findPossibleDuplicates(
			String name,
			String region,
			Double latitude,
			Double longitude) {
		String normalizedName = SuggestionNameNormalizer.normalize(name);
		String regionLower = region == null ? "" : region.trim().toLowerCase(Locale.ROOT);
		if (normalizedName.isBlank()) {
			return List.of();
		}

		Map<Long, PossibleDuplicateResponse> matches = new LinkedHashMap<>();
		for (Destination candidate : destinationRepository.findAllActive()) {
			String candidateName = SuggestionNameNormalizer.normalize(candidate.getName());
			String candidateRegion = candidate.getRegion() == null
					? ""
					: candidate.getRegion().trim().toLowerCase(Locale.ROOT);
			boolean sameRegion = !regionLower.isBlank() && regionLower.equals(candidateRegion);
			boolean nameExact = normalizedName.equals(candidateName);
			boolean nameOverlap = namesOverlap(normalizedName, candidateName);

			Double distanceKm = null;
			boolean proximate = false;
			if (latitude != null && longitude != null
					&& candidate.getLatitude() != null && candidate.getLongitude() != null) {
				distanceKm = round2(GeoUtils.haversineKm(
						latitude, longitude, candidate.getLatitude(), candidate.getLongitude()));
				proximate = distanceKm <= PROXIMITY_KM;
			}

			String reason = null;
			if (nameExact && sameRegion) {
				reason = "SAME_NAME_AND_REGION";
			}
			else if (nameExact && proximate) {
				reason = "SAME_NAME_AND_PROXIMITY";
			}
			else if (nameOverlap && sameRegion) {
				reason = "SIMILAR_NAME_AND_REGION";
			}
			else if (proximate && (nameOverlap || sameRegion)) {
				reason = "PROXIMITY_AND_CONTEXT";
			}

			if (reason != null) {
				matches.putIfAbsent(candidate.getId(), PossibleDuplicateResponse.from(candidate, reason, distanceKm));
			}
		}

		List<PossibleDuplicateResponse> result = new ArrayList<>(matches.values());
		result.sort(Comparator
				.comparing((PossibleDuplicateResponse d) -> d.distanceKm() == null)
				.thenComparing(d -> d.distanceKm() == null ? Double.MAX_VALUE : d.distanceKm())
				.thenComparing(PossibleDuplicateResponse::name, String.CASE_INSENSITIVE_ORDER));
		return List.copyOf(result);
	}

	private static boolean namesOverlap(String a, String b) {
		if (a.equals(b)) {
			return true;
		}
		if (a.length() < 4 || b.length() < 4) {
			return false;
		}
		return a.contains(b) || b.contains(a);
	}

	private static double round2(double value) {
		return Math.round(value * 100.0) / 100.0;
	}
}
