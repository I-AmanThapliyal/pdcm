package com.pdcm.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationRepository;
import com.pdcm.report.LocationVerificationResult;
import com.pdcm.report.TrafficCondition;
import com.pdcm.report.VerificationReason;
import com.pdcm.report.VerificationStatus;
import com.pdcm.report.VisitReport;
import com.pdcm.report.VisitReportRepository;
import com.pdcm.security.JwtService;
import com.pdcm.support.PostgresTestcontainers;
import com.pdcm.user.Role;
import com.pdcm.user.UserAccount;
import com.pdcm.user.UserAccountRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ModerationIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = PostgresTestcontainers.create();

	@DynamicPropertySource
	static void datasourceProps(DynamicPropertyRegistry registry) {
		PostgresTestcontainers.register(registry, postgres);
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	DestinationRepository destinationRepository;

	@Autowired
	VisitReportRepository visitReportRepository;

	@Autowired
	UserAccountRepository userAccountRepository;

	@Autowired
	JwtService jwtService;

	@Autowired
	ReportModerationEventRepository moderationEventRepository;

	@Test
	void nonAdminCannotAccessModerationApis() throws Exception {
		String userToken = register("mod-user@example.com");

		mockMvc.perform(get("/api/admin/reports").header("Authorization", "Bearer " + userToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/admin/reports/1/verify")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void anonymousCannotAccessModerationApis() throws Exception {
		mockMvc.perform(get("/api/admin/reports"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminCanVerifyFlaggedWithDistance_thenCrowdIncludesIt() throws Exception {
		Destination destination = destinationRepository.findAll().get(10);
		UserAccount contributor = userAccountRepository.save(new UserAccount(
				"flagged-contrib@example.com", "Contributor", "{noop}", Role.USER));
		UserAccount admin = userAccountRepository.save(new UserAccount(
				"mod-admin@example.com", "Admin", "{noop}", Role.ADMIN));
		String adminToken = jwtService.createAccessToken(admin);

		VisitReport flagged = new VisitReport(
				destination, contributor, 5, TrafficCondition.HEAVY, null, null, Instant.now());
		flagged.applyVerification(new LocationVerificationResult(
				VerificationStatus.FLAGGED,
				VerificationReason.SUSPICIOUS_COORDINATES,
				null,
				null,
				null,
				42.0,
				Instant.now()));
		flagged = visitReportRepository.save(flagged);

		mockMvc.perform(get("/api/destinations/{id}/crowd", destination.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contributingReports").value(0));

		mockMvc.perform(get("/api/admin/reports").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + flagged.getId() + ")].verificationStatus")
						.value(org.hamcrest.Matchers.hasItem("FLAGGED")))
				.andExpect(jsonPath("$[?(@.id == " + flagged.getId() + ")].clientLatitude").doesNotExist());

		mockMvc.perform(post("/api/admin/reports/{id}/verify", flagged.getId())
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"note":"Distance within geofence after review"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.report.verificationStatus").value("LOCATION_VERIFIED"))
				.andExpect(jsonPath("$.report.verificationReason").value("ADMIN_VERIFIED"))
				.andExpect(jsonPath("$.report.trustLabel").value("LOCATION_VERIFIED_SIGNAL"))
				.andExpect(jsonPath("$.retainedCoordinates").value(nullValue()))
				.andExpect(jsonPath("$.moderationHistory[0].fromStatus").value("FLAGGED"))
				.andExpect(jsonPath("$.moderationHistory[0].toStatus").value("LOCATION_VERIFIED"));

		mockMvc.perform(get("/api/destinations/{id}/crowd", destination.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contributingReports").value(1));

		assertThat(moderationEventRepository.findByReportIdOrderByCreatedAtDesc(flagged.getId()))
				.hasSize(1);
	}

	@Test
	void adminCannotVerifyLegacyUnverified() throws Exception {
		Destination destination = destinationRepository.findAll().get(11);
		UserAccount contributor = userAccountRepository.save(new UserAccount(
				"legacy-mod@example.com", "Legacy", "{noop}", Role.USER));
		UserAccount admin = userAccountRepository.save(new UserAccount(
				"legacy-admin@example.com", "Admin", "{noop}", Role.ADMIN));
		String adminToken = jwtService.createAccessToken(admin);

		VisitReport legacy = new VisitReport(
				destination, contributor, 4, TrafficCondition.MODERATE, null, null, Instant.now());
		legacy.setVerificationStatus(VerificationStatus.LEGACY_UNVERIFIED);
		legacy.setVerificationReason(VerificationReason.PRE_CUTOVER);
		legacy = visitReportRepository.save(legacy);

		mockMvc.perform(post("/api/admin/reports/{id}/verify", legacy.getId())
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isConflict());

		assertThat(visitReportRepository.findById(legacy.getId()).orElseThrow().getVerificationStatus())
				.isEqualTo(VerificationStatus.LEGACY_UNVERIFIED);
	}

	@Test
	void adminCannotVerifyWhenDistanceExceedsGeofence() throws Exception {
		Destination destination = destinationRepository.findAll().get(12);
		UserAccount contributor = userAccountRepository.save(new UserAccount(
				"far-mod@example.com", "Far", "{noop}", Role.USER));
		UserAccount admin = userAccountRepository.save(new UserAccount(
				"far-admin@example.com", "Admin", "{noop}", Role.ADMIN));
		String adminToken = jwtService.createAccessToken(admin);

		VisitReport pending = new VisitReport(
				destination, contributor, 3, TrafficCondition.LIGHT, null, null, Instant.now());
		pending.applyVerification(new LocationVerificationResult(
				VerificationStatus.PENDING,
				VerificationReason.OUTSIDE_GEOFENCE,
				null,
				null,
				null,
				2500.0,
				Instant.now()));
		pending = visitReportRepository.save(pending);

		mockMvc.perform(post("/api/admin/reports/{id}/verify", pending.getId())
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isConflict());
	}

	@Test
	void rejectRemovesVerifiedReportFromAggregation() throws Exception {
		Destination destination = destinationRepository.findAll().get(13);
		UserAccount contributor = userAccountRepository.save(new UserAccount(
				"reject-contrib@example.com", "Contributor", "{noop}", Role.USER));
		UserAccount admin = userAccountRepository.save(new UserAccount(
				"reject-admin@example.com", "Admin", "{noop}", Role.ADMIN));
		String adminToken = jwtService.createAccessToken(admin);

		VisitReport verified = new VisitReport(
				destination, contributor, 5, TrafficCondition.HEAVY, null, null, Instant.now());
		verified.applyVerification(new LocationVerificationResult(
				VerificationStatus.LOCATION_VERIFIED,
				VerificationReason.WITHIN_GEOFENCE,
				null,
				null,
				null,
				20.0,
				Instant.now()));
		verified = visitReportRepository.save(verified);

		mockMvc.perform(get("/api/destinations/{id}/crowd", destination.getId()))
				.andExpect(jsonPath("$.contributingReports").value(1));

		mockMvc.perform(post("/api/admin/reports/{id}/reject", verified.getId())
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"note":"Abuse"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.report.verificationStatus").value("REJECTED"));

		mockMvc.perform(get("/api/destinations/{id}/crowd", destination.getId()))
				.andExpect(jsonPath("$.contributingReports").value(0))
				.andExpect(jsonPath("$.status").value("UNKNOWN"));
	}

	@Test
	void adminDetailOmitsCoordinatesByDefault_evenWhenRetained() throws Exception {
		Destination destination = destinationRepository.findAll().get(14);
		UserAccount contributor = userAccountRepository.save(new UserAccount(
				"coords-contrib@example.com", "Contributor", "{noop}", Role.USER));
		UserAccount admin = userAccountRepository.save(new UserAccount(
				"coords-admin@example.com", "Admin", "{noop}", Role.ADMIN));
		String adminToken = jwtService.createAccessToken(admin);

		double lat = destination.getLatitude();
		double lon = destination.getLongitude();
		VisitReport flagged = new VisitReport(
				destination, contributor, 2, TrafficCondition.LIGHT, null, null, Instant.now());
		flagged.applyVerification(new LocationVerificationResult(
				VerificationStatus.FLAGGED,
				VerificationReason.SUSPICIOUS_COORDINATES,
				lat,
				lon,
				8.0,
				15.0,
				Instant.now()));
		flagged = visitReportRepository.save(flagged);

		mockMvc.perform(get("/api/admin/reports/{id}", flagged.getId())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.report.coordinatesRetained").value(true))
				.andExpect(jsonPath("$.retainedCoordinates").value(nullValue()))
				.andExpect(jsonPath("$.report.clientLatitude").doesNotExist());

		String body = mockMvc.perform(get("/api/admin/reports/{id}", flagged.getId())
						.param("includeCoordinates", "true")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.retainedCoordinates.latitude").value(lat))
				.andExpect(jsonPath("$.retainedCoordinates.longitude").value(lon))
				.andReturn()
				.getResponse()
				.getContentAsString();

		// Privacy: public-shaped fields must still not leak under alternate names on the summary.
		assertThat(body).doesNotContain("clientLatitude");
	}

	@Test
	void dashboardSeparatesPopularityFromVerifiedCrowdTrustLabels() throws Exception {
		Destination destination = destinationRepository.findAll().get(15);
		UserAccount contributor = userAccountRepository.save(new UserAccount(
				"dash-trust@example.com", "Contributor", "{noop}", Role.USER));

		VisitReport pending = new VisitReport(
				destination, contributor, 4, TrafficCondition.MODERATE, null, null, Instant.now());
		pending.setVerificationStatus(VerificationStatus.PENDING);
		pending.setVerificationReason(VerificationReason.NO_CONSENT);
		visitReportRepository.save(pending);

		mockMvc.perform(get("/api/dashboard"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.popularDestinations[0].recentReportCount").isNumber())
				.andExpect(jsonPath("$.popularDestinations[0].verifiedRecentReportCount").isNumber())
				.andExpect(jsonPath("$.recentReports[0].trustLabel").exists())
				.andExpect(jsonPath("$.recentReports[0].email").doesNotExist())
				.andExpect(jsonPath("$.recentReports[0].displayName").doesNotExist());
	}

	private String register(String email) throws Exception {
		String body = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","displayName":"Traveler","password":"password123"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode json = objectMapper.readTree(body);
		return json.get("accessToken").asText();
	}
}
