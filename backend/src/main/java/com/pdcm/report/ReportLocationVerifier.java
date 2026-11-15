package com.pdcm.report;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.pdcm.common.GeoUtils;
import com.pdcm.destination.Destination;

/**
 * GPS proximity check at submit time. LOCATION_VERIFIED means the sample was
 * inside the geofence with acceptable accuracy — not that the report content is true.
 */
@Component
public class ReportLocationVerifier {

	private final ReportVerificationProperties properties;
	private final Clock clock;

	public ReportLocationVerifier(ReportVerificationProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	public LocationVerificationResult verify(
			Destination destination,
			Boolean locationConsent,
			ClientLocationPayload clientLocation) {
		boolean consented = Boolean.TRUE.equals(locationConsent);
		if (!consented) {
			// Ignore any coordinates sent without consent (privacy).
			return LocationVerificationResult.pending(VerificationReason.NO_CONSENT);
		}

		Instant consentAt = clock.instant();
		if (clientLocation == null
				|| clientLocation.latitude() == null
				|| clientLocation.longitude() == null) {
			return LocationVerificationResult.pendingWithConsent(
					VerificationReason.LOCATION_UNAVAILABLE, consentAt);
		}

		double lat = clientLocation.latitude();
		double lon = clientLocation.longitude();
		Double accuracy = clientLocation.accuracyMeters();

		if (!Double.isFinite(lat) || !Double.isFinite(lon)
				|| lat < -90.0 || lat > 90.0
				|| lon < -180.0 || lon > 180.0) {
			return LocationVerificationResult.pendingWithConsent(
					VerificationReason.INVALID_COORDINATES, consentAt);
		}

		// Soft spoofing / glitch gate — not definitive proof of fraud.
		if ((Math.abs(lat) < 1e-6 && Math.abs(lon) < 1e-6)
				|| lat < properties.getIndiaLatMin()
				|| lat > properties.getIndiaLatMax()
				|| lon < properties.getIndiaLonMin()
				|| lon > properties.getIndiaLonMax()) {
			return LocationVerificationResult.flagged(
					VerificationReason.SUSPICIOUS_COORDINATES,
					consentAt,
					lat,
					lon,
					accuracy);
		}

		if (accuracy == null || !Double.isFinite(accuracy) || accuracy > properties.getMaxAccuracyMeters()) {
			return new LocationVerificationResult(
					VerificationStatus.PENDING,
					VerificationReason.ACCURACY_TOO_LOW,
					lat,
					lon,
					accuracy,
					distanceMeters(destination, lat, lon),
					consentAt);
		}

		double distanceM = distanceMeters(destination, lat, lon);
		if (distanceM > properties.getGeofenceRadiusMeters()) {
			return new LocationVerificationResult(
					VerificationStatus.PENDING,
					VerificationReason.OUTSIDE_GEOFENCE,
					lat,
					lon,
					accuracy,
					distanceM,
					consentAt);
		}

		return new LocationVerificationResult(
				VerificationStatus.LOCATION_VERIFIED,
				VerificationReason.WITHIN_GEOFENCE,
				lat,
				lon,
				accuracy,
				distanceM,
				consentAt);
	}

	private static double distanceMeters(Destination destination, double lat, double lon) {
		return GeoUtils.haversineMeters(
				destination.getLatitude(),
				destination.getLongitude(),
				lat,
				lon);
	}
}
