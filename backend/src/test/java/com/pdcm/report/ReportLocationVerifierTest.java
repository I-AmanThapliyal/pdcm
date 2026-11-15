package com.pdcm.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pdcm.destination.Destination;

class ReportLocationVerifierTest {

	private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

	private ReportLocationVerifier verifier;
	private Destination destination;

	@BeforeEach
	void setUp() {
		ReportVerificationProperties properties = new ReportVerificationProperties();
		verifier = new ReportLocationVerifier(properties, Clock.fixed(NOW, ZoneOffset.UTC));
		destination = mock(Destination.class);
		when(destination.getLatitude()).thenReturn(26.9855);
		when(destination.getLongitude()).thenReturn(75.8513);
	}

	@Test
	void withoutConsent_isPendingAndIgnoresCoordinates() {
		LocationVerificationResult result = verifier.verify(
				destination,
				false,
				new ClientLocationPayload(26.9855, 75.8513, 10.0));

		assertThat(result.status()).isEqualTo(VerificationStatus.PENDING);
		assertThat(result.reason()).isEqualTo(VerificationReason.NO_CONSENT);
		assertThat(result.clientLatitude()).isNull();
		assertThat(result.clientLongitude()).isNull();
	}

	@Test
	void consentWithoutLocation_isPendingUnavailable() {
		LocationVerificationResult result = verifier.verify(destination, true, null);

		assertThat(result.status()).isEqualTo(VerificationStatus.PENDING);
		assertThat(result.reason()).isEqualTo(VerificationReason.LOCATION_UNAVAILABLE);
		assertThat(result.locationConsentAt()).isEqualTo(NOW);
	}

	@Test
	void withinGeofence_isLocationVerified() {
		LocationVerificationResult result = verifier.verify(
				destination,
				true,
				new ClientLocationPayload(26.9857, 75.8514, 20.0));

		assertThat(result.status()).isEqualTo(VerificationStatus.LOCATION_VERIFIED);
		assertThat(result.reason()).isEqualTo(VerificationReason.WITHIN_GEOFENCE);
		assertThat(result.distanceToDestinationMeters()).isLessThan(100.0);
		assertThat(result.clientLatitude()).isEqualTo(26.9857);
	}

	@Test
	void outsideGeofence_isPending() {
		LocationVerificationResult result = verifier.verify(
				destination,
				true,
				new ClientLocationPayload(26.9000, 75.8000, 15.0));

		assertThat(result.status()).isEqualTo(VerificationStatus.PENDING);
		assertThat(result.reason()).isEqualTo(VerificationReason.OUTSIDE_GEOFENCE);
		assertThat(result.distanceToDestinationMeters()).isGreaterThan(500.0);
	}

	@Test
	void poorAccuracy_isPending() {
		LocationVerificationResult result = verifier.verify(
				destination,
				true,
				new ClientLocationPayload(26.9855, 75.8513, 250.0));

		assertThat(result.status()).isEqualTo(VerificationStatus.PENDING);
		assertThat(result.reason()).isEqualTo(VerificationReason.ACCURACY_TOO_LOW);
	}

	@Test
	void nullIsland_isFlaggedSuspicious() {
		LocationVerificationResult result = verifier.verify(
				destination,
				true,
				new ClientLocationPayload(0.0, 0.0, 5.0));

		assertThat(result.status()).isEqualTo(VerificationStatus.FLAGGED);
		assertThat(result.reason()).isEqualTo(VerificationReason.SUSPICIOUS_COORDINATES);
	}
}
