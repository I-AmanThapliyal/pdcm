package com.pdcm.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.pdcm.destination.Destination;
import com.pdcm.user.Role;
import com.pdcm.user.UserAccount;

class VisitReportPurgeUnitTest {

	@Test
	void purgeClearsCoordsOnceAndPreservesMetadata() {
		VisitReport report = new VisitReport(
				mock(Destination.class),
				new UserAccount("u@example.com", "U", "x", Role.USER),
				3,
				TrafficCondition.MODERATE,
				null,
				"note",
				Instant.parse("2026-11-14T10:00:00Z"));
		report.applyVerification(new LocationVerificationResult(
				VerificationStatus.LOCATION_VERIFIED,
				VerificationReason.WITHIN_GEOFENCE,
				26.9,
				75.8,
				11.0,
				40.0,
				Instant.parse("2026-11-14T10:00:00Z")));

		Instant purgedAt = Instant.parse("2026-11-15T10:00:00Z");
		assertThat(report.purgeClientCoordinates(purgedAt)).isTrue();
		assertThat(report.getClientLatitude()).isNull();
		assertThat(report.getClientLongitude()).isNull();
		assertThat(report.getClientAccuracyMeters()).isNull();
		assertThat(report.getLocationPurgedAt()).isEqualTo(purgedAt);
		assertThat(report.getVerificationStatus()).isEqualTo(VerificationStatus.LOCATION_VERIFIED);
		assertThat(report.getDistanceToDestinationMeters()).isEqualTo(40.0);
		assertThat(report.getNote()).isEqualTo("note");

		assertThat(report.purgeClientCoordinates(Instant.parse("2026-11-16T10:00:00Z"))).isFalse();
		assertThat(report.getLocationPurgedAt()).isEqualTo(purgedAt);
	}
}
