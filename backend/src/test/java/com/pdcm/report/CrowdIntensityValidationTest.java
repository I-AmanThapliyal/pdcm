package com.pdcm.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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

import com.pdcm.destination.DestinationRepository;
import com.pdcm.support.PostgresTestcontainers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CrowdIntensityValidationTest {

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

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 3, 4, 5 })
	void acceptsWholeNumbersOneThroughFive(int intensity) throws Exception {
		String token = registerAndLogin("crowd-ok-" + intensity + "@example.com");
		Long destinationId = firstDestinationId();
		long before = visitReportRepository.count();

		mockMvc.perform(post("/api/destinations/{id}/reports", destinationId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportBody(intensity)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.crowdIntensity").value(intensity));

		assertThat(visitReportRepository.count()).isEqualTo(before + 1);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, -1, 6, 9 })
	void rejectsOutOfRangeIntegers(int intensity) throws Exception {
		String token = registerAndLogin("crowd-bad-" + Math.abs(intensity) + "-" + (intensity < 0 ? "n" : "p") + "@example.com");
		Long destinationId = firstDestinationId();
		long before = visitReportRepository.count();

		mockMvc.perform(post("/api/destinations/{id}/reports", destinationId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportBody(intensity)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		assertThat(visitReportRepository.count()).isEqualTo(before);
	}

	@ParameterizedTest
	@MethodSource("invalidBodies")
	void rejectsNonIntegerNullAndMissing(String body) throws Exception {
		String token = registerAndLogin("crowd-type-" + Math.abs(body.hashCode()) + "@example.com");
		Long destinationId = firstDestinationId();
		long before = visitReportRepository.count();

		mockMvc.perform(post("/api/destinations/{id}/reports", destinationId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		assertThat(visitReportRepository.count()).isEqualTo(before);
	}

	@Test
	void validReportStillWorksAfterRejectingInvalidIntensity() throws Exception {
		String token = registerAndLogin("crowd-regression@example.com");
		Long destinationId = firstDestinationId();

		mockMvc.perform(post("/api/destinations/{id}/reports", destinationId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportBody(9)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/destinations/{id}/reports", destinationId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportBody(4)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.crowdIntensity").value(4));
	}

	static Stream<String> invalidBodies() {
		String observedAt = Instant.now().toString();
		return Stream.of(
				"""
						{"crowdIntensity":1.5,"trafficCondition":"LIGHT","observedAt":"%s"}
						""".formatted(observedAt),
				"""
						{"crowdIntensity":null,"trafficCondition":"LIGHT","observedAt":"%s"}
						""".formatted(observedAt),
				"""
						{"trafficCondition":"LIGHT","observedAt":"%s"}
						""".formatted(observedAt));
	}

	private Long firstDestinationId() {
		return destinationRepository.findAll().getFirst().getId();
	}

	private String registerAndLogin(String email) throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","displayName":"Reporter","password":"password123"}
								""".formatted(email)))
				.andExpect(status().isCreated());

		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"password123"}
								""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
		return body.get("accessToken").asText();
	}

	private String reportBody(int intensity) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("crowdIntensity", intensity);
		node.put("trafficCondition", "LIGHT");
		node.put("observedAt", Instant.now().toString());
		return node.toString();
	}
}
