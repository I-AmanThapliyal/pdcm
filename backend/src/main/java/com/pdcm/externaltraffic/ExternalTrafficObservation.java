package com.pdcm.externaltraffic;

import java.time.Instant;

/**
 * In-memory only. Do not persist TomTom payloads until licence review allows it.
 * Phase 0 research: TomTom terms restrict long-term storage / derivative databases;
 * short-lived in-process cache is the safe default.
 */
public record ExternalTrafficObservation(
		Long destinationId,
		String provider,
		ExternalTrafficStatus status,
		Integer currentSpeedKph,
		Integer freeFlowSpeedKph,
		Double providerConfidence,
		String functionalRoadClass,
		Instant fetchedAt,
		Instant expiresAt,
		String attribution,
		String message,
		boolean coverageAvailable,
		String coverageNote,
		String interpretation
) {
	static final String SINGLE_ROAD_COVERAGE_NOTE =
			"One nearby approach road segment only — not destination-wide coverage.";

	static ExternalTrafficObservation unavailable(
			Long destinationId,
			String provider,
			Instant fetchedAt,
			Instant expiresAt,
			String attribution,
			String message) {
		return new ExternalTrafficObservation(
				destinationId,
				provider,
				ExternalTrafficStatus.UNAVAILABLE,
				null,
				null,
				null,
				null,
				fetchedAt,
				expiresAt,
				attribution,
				message,
				false,
				null,
				null);
	}

	static ExternalTrafficObservation unknown(
			Long destinationId,
			String provider,
			Instant fetchedAt,
			Instant expiresAt,
			String attribution,
			String message) {
		return new ExternalTrafficObservation(
				destinationId,
				provider,
				ExternalTrafficStatus.UNKNOWN,
				null,
				null,
				null,
				null,
				fetchedAt,
				expiresAt,
				attribution,
				message,
				false,
				SINGLE_ROAD_COVERAGE_NOTE,
				"PDCM interpretation: unknown — no usable nearby road sample.");
	}
}
