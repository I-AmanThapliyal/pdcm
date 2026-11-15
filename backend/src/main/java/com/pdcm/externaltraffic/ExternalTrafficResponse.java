package com.pdcm.externaltraffic;

import java.time.Instant;

public record ExternalTrafficResponse(
		Long destinationId,
		String channel,
		String provider,
		ExternalTrafficStatus status,
		Integer currentSpeedKph,
		Integer freeFlowSpeedKph,
		Double providerConfidence,
		String functionalRoadClass,
		Instant fetchedAt,
		Instant expiresAt,
		String freshness,
		String attribution,
		String message,
		boolean pilotEnabled,
		boolean coverageAvailable,
		String coverageNote,
		String interpretation
) {
	public static ExternalTrafficResponse from(ExternalTrafficObservation observation, boolean pilotEnabled) {
		Instant now = Instant.now();
		String freshness;
		if (observation.status() == ExternalTrafficStatus.UNAVAILABLE) {
			freshness = "NONE";
		} else if (observation.expiresAt().isBefore(now)) {
			freshness = "STALE";
		} else {
			freshness = "FRESH";
		}
		return new ExternalTrafficResponse(
				observation.destinationId(),
				"EXTERNAL_ROAD",
				observation.provider(),
				observation.status(),
				observation.currentSpeedKph(),
				observation.freeFlowSpeedKph(),
				observation.providerConfidence(),
				observation.functionalRoadClass(),
				observation.fetchedAt(),
				observation.expiresAt(),
				freshness,
				observation.attribution(),
				observation.message(),
				pilotEnabled,
				observation.coverageAvailable(),
				observation.coverageNote(),
				observation.interpretation());
	}
}
