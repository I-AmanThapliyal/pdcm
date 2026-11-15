package com.pdcm.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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

import com.pdcm.destination.DestinationRepository;
import com.pdcm.support.PostgresTestcontainers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class VisitReportIntegrationTest {

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

	@Test
	void submitReport_success_andAnonymousForbidden() throws Exception {
		String token = registerAndLogin("reporter1@example.com");
		Long destinationId = firstDestinationId();
		String body = reportJson(3, "MODERATE", Instant.now());

		mockMvc.perform(post("/api/destinations/{id}/reports", destinationId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/destinations/{id}/reports", destinationId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.crowdIntensity").value(3))
				.andExpect(jsonPath("$.trafficCondition").value("MODERATE"))
				.andExpect(jsonPath("$.destinationId").value(destinationId))
				.andExpect(jsonPath("$.userId").exists())
				.andExpect(jsonPath("$.verificationStatus").value("PENDING"))
				.andExpect(jsonPath("$.verificationReason").value("NO_CONSENT"))
				.andExpect(jsonPath("$.clientLatitude").doesNotExist())
				.andExpect(jsonPath("$.clientLongitude").doesNotExist());
	}

	@Test
	void submitReport_invalidObservedAt_andUnknownDestination() throws Exception {
		String token = registerAndLogin("reporter2@example.com");
		Long destinationId = firstDestinationId();

		mockMvc.perform(post("/api/destinations/{id}/reports", destinationId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportJson(2, "LIGHT", Instant.now().minusSeconds(25 * 3600))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/destinations/{id}/reports", 999999)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportJson(2, "LIGHT", Instant.now())))
				.andExpect(status().isNotFound());
	}

	@Test
	void cooldown_blocksSecondReportForSameDestination() throws Exception {
		String token = registerAndLogin("cooldown@example.com");
		Long destinationId = firstDestinationId();
		String body = reportJson(4, "HEAVY", Instant.now());

		mockMvc.perform(post("/api/destinations/{id}/reports", destinationId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/destinations/{id}/reports", destinationId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("REPORT_RATE_LIMITED"))
				.andExpect(jsonPath("$.limit").value("COOLDOWN_30M"));
	}

	@Test
	void dailyCap_acrossDifferentDestinations() throws Exception {
		String token = registerAndLogin("dailycap@example.com");
		List<Long> destinationIds = destinationRepository.findAll().stream()
				.limit(11)
				.map(d -> d.getId())
				.toList();
		assertThat(destinationIds).hasSizeGreaterThanOrEqualTo(11);

		for (int i = 0; i < 10; i++) {
			mockMvc.perform(post("/api/destinations/{id}/reports", destinationIds.get(i))
							.header("Authorization", "Bearer " + token)
							.contentType(MediaType.APPLICATION_JSON)
							.content(reportJson(2, "LIGHT", Instant.now())))
					.andExpect(status().isCreated());
		}

		mockMvc.perform(post("/api/destinations/{id}/reports", destinationIds.get(10))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportJson(2, "LIGHT", Instant.now())))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.limit").value("DAILY_10"));
	}

	@Test
	void concurrentSubmissions_cannotExceedDailyCap() throws Exception {
		String token = registerAndLogin("concurrent@example.com");
		List<Long> destinationIds = destinationRepository.findAll().stream()
				.limit(20)
				.map(d -> d.getId())
				.toList();

		ExecutorService pool = Executors.newFixedThreadPool(12);
		AtomicInteger created = new AtomicInteger();
		AtomicInteger limited = new AtomicInteger();
		try {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int i = 0; i < 12; i++) {
				Long destinationId = destinationIds.get(i);
				tasks.add(() -> {
					MvcResult result = mockMvc.perform(post("/api/destinations/{id}/reports", destinationId)
									.header("Authorization", "Bearer " + token)
									.contentType(MediaType.APPLICATION_JSON)
									.content(reportJson(3, "MODERATE", Instant.now())))
							.andReturn();
					int status = result.getResponse().getStatus();
					if (status == 201) {
						created.incrementAndGet();
					}
					else if (status == 429) {
						limited.incrementAndGet();
					}
					else {
						throw new IllegalStateException("Unexpected status " + status + ": "
								+ result.getResponse().getContentAsString());
					}
					return null;
				});
			}
			List<Future<Void>> futures = pool.invokeAll(tasks);
			for (Future<Void> future : futures) {
				future.get();
			}
		}
		finally {
			pool.shutdownNow();
		}

		assertThat(created.get()).isEqualTo(10);
		assertThat(limited.get()).isEqualTo(2);
		assertThat(visitReportRepository.count()).isGreaterThanOrEqualTo(10);
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

	private String reportJson(int intensity, String traffic, Instant observedAt) throws Exception {
		return objectMapper.createObjectNode()
				.put("crowdIntensity", intensity)
				.put("trafficCondition", traffic)
				.put("observedAt", observedAt.toString())
				.toString();
	}
}
