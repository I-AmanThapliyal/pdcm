package com.pdcm.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationRepository;
import com.pdcm.security.JwtService;
import com.pdcm.support.PostgresTestcontainers;
import com.pdcm.user.Role;
import com.pdcm.user.UserAccount;
import com.pdcm.user.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class LocationCoordinatePurgeIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = PostgresTestcontainers.create();

	@DynamicPropertySource
	static void datasourceProps(DynamicPropertyRegistry registry) {
		PostgresTestcontainers.register(registry, postgres);
		registry.add("pdcm.report-verification.coordinate-retention", () -> "PT24H");
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	DestinationRepository destinationRepository;

	@Autowired
	VisitReportRepository visitReportRepository;

	@Autowired
	UserAccountRepository userAccountRepository;

	@Autowired
	LocationCoordinatePurgeService purgeService;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	JwtService jwtService;

	@Test
	void purgeClearsCoordsPastRetention_preservesVerificationMetadata_andIsIdempotent() {
		Destination destination = destinationRepository.findAll().get(16);
		UserAccount user = userAccountRepository.save(new UserAccount(
				"purge-old@example.com", "Purge", "{noop}", Role.USER));

		VisitReport oldReport = new VisitReport(
				destination, user, 3, TrafficCondition.MODERATE, null, null, Instant.now());
		oldReport.applyVerification(new LocationVerificationResult(
				VerificationStatus.LOCATION_VERIFIED,
				VerificationReason.WITHIN_GEOFENCE,
				destination.getLatitude(),
				destination.getLongitude(),
				12.0,
				25.0,
				Instant.now()));
		oldReport = visitReportRepository.saveAndFlush(oldReport);

		VisitReport freshReport = new VisitReport(
				destination,
				userAccountRepository.save(new UserAccount(
						"purge-fresh@example.com", "Fresh", "{noop}", Role.USER)),
				2,
				TrafficCondition.LIGHT,
				null,
				null,
				Instant.now());
		freshReport.applyVerification(new LocationVerificationResult(
				VerificationStatus.LOCATION_VERIFIED,
				VerificationReason.WITHIN_GEOFENCE,
				destination.getLatitude() + 0.0001,
				destination.getLongitude() + 0.0001,
				10.0,
				18.0,
				Instant.now()));
		freshReport = visitReportRepository.saveAndFlush(freshReport);

		Instant agedCreatedAt = Instant.now().minus(25, ChronoUnit.HOURS);
		jdbcTemplate.update(
				"UPDATE visit_reports SET created_at = ? WHERE id = ?",
				java.sql.Timestamp.from(agedCreatedAt),
				oldReport.getId());

		assertThat(purgeService.purgeExpired()).isGreaterThanOrEqualTo(1);
		visitReportRepository.flush();

		VisitReport purged = visitReportRepository.findById(oldReport.getId()).orElseThrow();
		assertThat(purged.getClientLatitude()).isNull();
		assertThat(purged.getClientLongitude()).isNull();
		assertThat(purged.getClientAccuracyMeters()).isNull();
		assertThat(purged.getLocationPurgedAt()).isNotNull();
		assertThat(purged.getVerificationStatus()).isEqualTo(VerificationStatus.LOCATION_VERIFIED);
		assertThat(purged.getVerificationReason()).isEqualTo(VerificationReason.WITHIN_GEOFENCE);
		assertThat(purged.getDistanceToDestinationMeters()).isEqualTo(25.0);

		VisitReport stillFresh = visitReportRepository.findById(freshReport.getId()).orElseThrow();
		assertThat(stillFresh.hasRetainedClientCoordinates()).isTrue();
		assertThat(stillFresh.getLocationPurgedAt()).isNull();

		int secondPass = purgeService.purgeExpired();
		assertThat(secondPass).isEqualTo(0);

		VisitReport stillPurged = visitReportRepository.findById(oldReport.getId()).orElseThrow();
		assertThat(stillPurged.getLocationPurgedAt()).isEqualTo(purged.getLocationPurgedAt());
	}

	@Test
	void publicAndAdminApisNeverExposeCoordsAfterPurge() throws Exception {
		Destination destination = destinationRepository.findAll().get(17);
		UserAccount user = userAccountRepository.save(new UserAccount(
				"purge-api@example.com", "PurgeApi", "{noop}", Role.USER));
		UserAccount admin = userAccountRepository.save(new UserAccount(
				"purge-admin@example.com", "Admin", "{noop}", Role.ADMIN));

		VisitReport report = new VisitReport(
				destination, user, 4, TrafficCondition.HEAVY, null, null, Instant.now());
		report.applyVerification(new LocationVerificationResult(
				VerificationStatus.FLAGGED,
				VerificationReason.SUSPICIOUS_COORDINATES,
				0.0,
				0.0,
				5.0,
				null,
				Instant.now()));
		report = visitReportRepository.saveAndFlush(report);

		jdbcTemplate.update(
				"UPDATE visit_reports SET created_at = ? WHERE id = ?",
				java.sql.Timestamp.from(Instant.now().minus(30, ChronoUnit.HOURS)),
				report.getId());
		purgeService.purgeExpired();

		String adminBody = mockMvc.perform(get("/api/admin/reports/{id}", report.getId())
						.param("includeCoordinates", "true")
						.header("Authorization", "Bearer " + jwtService.createAccessToken(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.report.coordinatesRetained").value(false))
				.andExpect(jsonPath("$.retainedCoordinates").value(nullValue()))
				.andExpect(jsonPath("$.report.verificationStatus").value("FLAGGED"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(adminBody.toLowerCase()).doesNotContain("\"latitude\"").doesNotContain("\"longitude\"");
	}
}
