package com.pdcm.report;

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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationRepository;
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
class GpsVerificationIntegrationTest {

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

	@Test
	void locationVerified_contributesToCrowd_andResponseOmitsCoordinates() throws Exception {
		Destination destination = destinationRepository.findAll().get(4);
		String token = register("gps-ok@example.com");

		MvcResult created = mockMvc.perform(post("/api/destinations/{id}/reports", destination.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(verifiedJson(destination, 4, "HEAVY")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.verificationStatus").value("LOCATION_VERIFIED"))
				.andExpect(jsonPath("$.verificationReason").value("WITHIN_GEOFENCE"))
				.andExpect(jsonPath("$.clientLatitude").doesNotExist())
				.andExpect(jsonPath("$.clientLongitude").doesNotExist())
				.andExpect(jsonPath("$.clientAccuracyMeters").doesNotExist())
				.andExpect(jsonPath("$.latitude").doesNotExist())
				.andExpect(jsonPath("$.longitude").doesNotExist())
				.andReturn();

		String responseJson = created.getResponse().getContentAsString();
		assertThat(responseJson).doesNotContain("clientLatitude", "clientLongitude", "accuracyMeters");
		assertThat(responseJson.toLowerCase()).doesNotContain("\"latitude\"").doesNotContain("\"longitude\"");

		mockMvc.perform(get("/api/destinations/{id}/crowd", destination.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CROWDED"))
				.andExpect(jsonPath("$.contributingReports").value(1));
	}

	@Test
	void pendingWithoutGps_doesNotContributeToCrowd() throws Exception {
		Destination destination = destinationRepository.findAll().get(5);
		String token = register("gps-pending@example.com");

		mockMvc.perform(post("/api/destinations/{id}/reports", destination.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"crowdIntensity":5,"trafficCondition":"HEAVY","observedAt":"%s"}
								""".formatted(Instant.now())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.verificationStatus").value("PENDING"))
				.andExpect(jsonPath("$.verificationReason").value("NO_CONSENT"));

		mockMvc.perform(get("/api/destinations/{id}/crowd", destination.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.score").value(nullValue()))
				.andExpect(jsonPath("$.contributingReports").value(0));
	}

	@Test
	void outsideGeofence_isPendingAndExcluded() throws Exception {
		Destination destination = destinationRepository.findAll().get(6);
		String token = register("gps-far@example.com");
		// ~2 km away — outside 500 m geofence, still inside soft India bounds.
		double farLat = destination.getLatitude() + 0.02;
		double farLon = destination.getLongitude() + 0.02;

		mockMvc.perform(post("/api/destinations/{id}/reports", destination.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "crowdIntensity":5,
								  "trafficCondition":"HEAVY",
								  "observedAt":"%s",
								  "locationConsent":true,
								  "clientLocation":{"latitude":%s,"longitude":%s,"accuracyMeters":10.0}
								}
								""".formatted(Instant.now(), farLat, farLon)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.verificationStatus").value("PENDING"))
				.andExpect(jsonPath("$.verificationReason").value("OUTSIDE_GEOFENCE"));

		mockMvc.perform(get("/api/destinations/{id}/crowd", destination.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contributingReports").value(0));
	}

	@Test
	void legacyUnverified_isPreservedAndExcludedFromAggregation() {
		Destination destination = destinationRepository.findAll().get(7);
		UserAccount user = userAccountRepository.save(new UserAccount(
				"legacy@example.com", "Legacy", "{noop}", Role.USER));
		VisitReport legacy = new VisitReport(
				destination,
				user,
				5,
				TrafficCondition.HEAVY,
				null,
				null,
				Instant.now());
		legacy.setVerificationStatus(VerificationStatus.LEGACY_UNVERIFIED);
		legacy.setVerificationReason(VerificationReason.PRE_CUTOVER);
		visitReportRepository.save(legacy);

		VisitReport stored = visitReportRepository.findById(legacy.getId()).orElseThrow();
		assertThat(stored.getVerificationStatus()).isEqualTo(VerificationStatus.LEGACY_UNVERIFIED);

		assertThat(visitReportRepository.findFreshByDestination(
				destination.getId(), Instant.now().minusSeconds(3600))).isEmpty();
	}

	private String verifiedJson(Destination destination, int intensity, String traffic) {
		return """
				{
				  "crowdIntensity":%d,
				  "trafficCondition":"%s",
				  "observedAt":"%s",
				  "locationConsent":true,
				  "clientLocation":{
				    "latitude":%s,
				    "longitude":%s,
				    "accuracyMeters":12.0
				  }
				}
				""".formatted(
				intensity,
				traffic,
				Instant.now(),
				destination.getLatitude(),
				destination.getLongitude());
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
