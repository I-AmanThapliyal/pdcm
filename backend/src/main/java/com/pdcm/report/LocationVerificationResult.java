package com.pdcm.report;

import java.time.Instant;

/**
 * Server-side verification outcome. Coordinates are retained only when consent
 * and a usable sample were provided; they must never appear on public DTOs.
 */
public record LocationVerificationResult(
		VerificationStatus status,
		VerificationReason reason,
		Double clientLatitude,
		Double clientLongitude,
		Double clientAccuracyMeters,
		Double distanceToDestinationMeters,
		Instant locationConsentAt
) {
	public static LocationVerificationResult pending(VerificationReason reason) {
		return new LocationVerificationResult(VerificationStatus.PENDING, reason, null, null, null, null, null);
	}

	public static LocationVerificationResult pendingWithConsent(
			VerificationReason reason,
			Instant consentAt) {
		return new LocationVerificationResult(
				VerificationStatus.PENDING, reason, null, null, null, null, consentAt);
	}

	public static LocationVerificationResult flagged(
			VerificationReason reason,
			Instant consentAt,
			Double lat,
			Double lon,
			Double accuracyMeters) {
		return new LocationVerificationResult(
				VerificationStatus.FLAGGED, reason, lat, lon, accuracyMeters, null, consentAt);
	}
}
