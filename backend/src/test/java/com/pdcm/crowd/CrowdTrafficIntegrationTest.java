package com.pdcm.crowd;

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
import com.pdcm.support.PostgresTestcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CrowdTrafficIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = PostgresTestcontainers.create();

	@DynamicPropertySource
	static void datasourceProps(DynamicPropertyRegistry registry) {
		PostgresTestcontainers.register(registry, postgres);
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	DestinationRepository destinationRepository;

	@Test
	void emptyDestination_isUnknownNotQuiet() throws Exception {
		Long destinationId = destinationRepository.findAll().getFirst().getId();

		mockMvc.perform(get("/api/destinations/{id}/crowd", destinationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.score").value(nullValue()))
				.andExpect(jsonPath("$.confidence").value("NONE"))
				.andExpect(jsonPath("$.contributingReports").value(0))
				.andExpect(jsonPath("$.source").value("USER_REPORTS"));

		mockMvc.perform(get("/api/destinations/{id}/traffic", destinationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.confidence").value("NONE"));
	}

	@Test
	void freshReports_driveCrowdAndTraffic() throws Exception {
		Long destinationId = destinationRepository.findAll().get(1).getId();
		submit("crowd-a@example.com", destinationId, 5, "HEAVY");
		submit("crowd-b@example.com", destinationId, 4, "HEAVY");

		mockMvc.perform(get("/api/destinations/{id}/crowd", destinationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CROWDED"))
				.andExpect(jsonPath("$.score").value(4.5))
				.andExpect(jsonPath("$.confidence").value("MEDIUM"))
				.andExpect(jsonPath("$.contributingReports").value(2))
				.andExpect(jsonPath("$.lastObservedAt").exists())
				.andExpect(jsonPath("$.asOf").exists());

		mockMvc.perform(get("/api/destinations/{id}/traffic", destinationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("HEAVY"))
				.andExpect(jsonPath("$.confidence").value("MEDIUM"))
				.andExpect(jsonPath("$.contributingReports").value(2));
	}

	@Test
	void staleObservation_doesNotCountAsCurrent() throws Exception {
		Destination destination = destinationRepository.findAll().get(2);
		String token = register("stale@example.com");
		String observedAt = Instant.now().minusSeconds(7 * 3600).toString();

		mockMvc.perform(post("/api/destinations/{id}/reports", destination.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(verifiedBody(destination, 5, "HEAVY", observedAt)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/destinations/{id}/crowd", destination.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.contributingReports").value(0));
	}

	@Test
	void unknownDestination_isNotFound() throws Exception {
		mockMvc.perform(get("/api/destinations/{id}/crowd", 999999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	private void submit(String email, Long destinationId, int intensity, String traffic) throws Exception {
		Destination destination = destinationRepository.findById(destinationId).orElseThrow();
		String token = register(email);
		mockMvc.perform(post("/api/destinations/{id}/reports", destinationId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(verifiedBody(destination, intensity, traffic, Instant.now().toString())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.verificationStatus").value("LOCATION_VERIFIED"));
	}

	private static String verifiedBody(Destination destination, int intensity, String traffic, String observedAt) {
		return """
				{
				  "crowdIntensity":%d,
				  "trafficCondition":"%s",
				  "observedAt":"%s",
				  "locationConsent":true,
				  "clientLocation":{"latitude":%s,"longitude":%s,"accuracyMeters":12.0}
				}
				""".formatted(
				intensity,
				traffic,
				observedAt,
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
		int start = body.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
		return body.substring(start, body.indexOf('"', start));
	}
}
