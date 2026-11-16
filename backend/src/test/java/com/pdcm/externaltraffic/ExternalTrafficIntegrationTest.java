package com.pdcm.externaltraffic;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
class ExternalTrafficIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = PostgresTestcontainers.create();

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry registry) {
		PostgresTestcontainers.register(registry, postgres);
		registry.add("pdcm.external-traffic.api-key", () -> "test-tomtom-key");
		registry.add("pdcm.external-traffic.schedule-enabled", () -> "false");
		registry.add("pdcm.external-traffic.daily-request-budget", () -> "20");
		registry.add("pdcm.external-traffic.monthly-request-budget", () -> "15000");
		registry.add("pdcm.external-traffic.cache-ttl-minutes", () -> "180");
		registry.add("pdcm.external-traffic.max-segment-distance-meters", () -> "750");
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	DestinationRepository destinationRepository;

	@Autowired
	ExternalTrafficCache cache;

	@Autowired
	ExternalTrafficService externalTrafficService;

	@Autowired
	ProviderRequestStatsRepository requestStatsRepository;

	@Autowired
	ProviderHealthRepository healthRepository;

	@Autowired
	ProviderBudgetService budgetService;

	@MockitoBean
	TrafficProvider trafficProvider;

	@BeforeEach
	void setUp() {
		cache.clear();
		requestStatsRepository.deleteAll();
		healthRepository.deleteAll();
		healthRepository.save(new ProviderHealth("TOMTOM", ProviderHealthStatus.UP));
		when(trafficProvider.providerId()).thenReturn("TOMTOM");
	}

	@Test
	void firstRequest_callsProviderOnce_thenCacheWithinTtl() throws Exception {
		Destination pilot = destinationRepository.findBySlugAndActiveTrue("india-gate").orElseThrow();
		when(trafficProvider.fetchFlow(anyDouble(), anyDouble(), anyInt()))
				.thenReturn(nearReading(pilot, 20, 60, "FRC3"));

		Long id = pilot.getId();

		mockMvc.perform(get("/api/destinations/{id}/traffic/external", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.channel").value("EXTERNAL_ROAD"))
				.andExpect(jsonPath("$.provider").value("TOMTOM"))
				.andExpect(jsonPath("$.status").value("HEAVY"))
				.andExpect(jsonPath("$.pilotEnabled").value(true))
				.andExpect(jsonPath("$.currentSpeedKph").value(20))
				.andExpect(jsonPath("$.coverageAvailable").value(true))
				.andExpect(jsonPath("$.coverageNote", containsString("not destination-wide")))
				.andExpect(jsonPath("$.interpretation", containsString("PDCM interpretation")))
				.andExpect(jsonPath("$.attribution").exists());

		mockMvc.perform(get("/api/destinations/{id}/traffic/external", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("HEAVY"))
				.andExpect(jsonPath("$.freshness").value("FRESH"));

		verify(trafficProvider, times(1)).fetchFlow(anyDouble(), anyDouble(), anyInt());
	}

	@Test
	void expiredCache_triggersNewProviderCall() throws Exception {
		Destination pilot = destinationRepository.findBySlugAndActiveTrue("india-gate").orElseThrow();
		when(trafficProvider.fetchFlow(anyDouble(), anyDouble(), anyInt()))
				.thenReturn(nearReading(pilot, 40, 60, "FRC2"));

		Instant past = Instant.parse("2020-01-01T00:00:00Z");
		cache.put(new ExternalTrafficObservation(
				pilot.getId(),
				"TOMTOM",
				ExternalTrafficStatus.LIGHT,
				55,
				60,
				0.9,
				"FRC2",
				past,
				past.plusSeconds(60),
				"Traffic data © TomTom",
				null,
				true,
				ExternalTrafficObservation.SINGLE_ROAD_COVERAGE_NOTE,
				"stale"));

		mockMvc.perform(get("/api/destinations/{id}/traffic/external", pilot.getId()).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("MODERATE"))
				.andExpect(jsonPath("$.currentSpeedKph").value(40));

		verify(trafficProvider, times(1)).fetchFlow(anyDouble(), anyDouble(), anyInt());
	}

	@Test
	void concurrentSameDestination_singleProviderCall() throws Exception {
		Destination pilot = destinationRepository.findBySlugAndActiveTrue("india-gate").orElseThrow();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicInteger calls = new AtomicInteger();

		when(trafficProvider.fetchFlow(anyDouble(), anyDouble(), anyInt())).thenAnswer((Answer<FlowSegmentReading>) invocation -> {
			calls.incrementAndGet();
			started.countDown();
			if (!release.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("release timeout");
			}
			return nearReading(pilot, 50, 60, "FRC2");
		});

		Long id = pilot.getId();
		int threads = 8;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		List<Future<ExternalTrafficResponse>> futures = new ArrayList<>();
		try {
			for (int i = 0; i < threads; i++) {
				futures.add(pool.submit(() -> externalTrafficService.current(id)));
			}
			if (!started.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("provider not entered");
			}
			release.countDown();
			for (Future<ExternalTrafficResponse> future : futures) {
				ExternalTrafficResponse response = future.get(15, TimeUnit.SECONDS);
				org.assertj.core.api.Assertions.assertThat(response.status()).isEqualTo(ExternalTrafficStatus.LIGHT);
			}
		} finally {
			pool.shutdownNow();
		}

		verify(trafficProvider, times(1)).fetchFlow(anyDouble(), anyDouble(), anyInt());
		org.assertj.core.api.Assertions.assertThat(calls.get()).isEqualTo(1);
	}

	@Test
	void zoomFallback_stopsAtFirstValidSegment() throws Exception {
		Destination pilot = destinationRepository.findBySlugAndActiveTrue("india-gate").orElseThrow();
		when(trafficProvider.fetchFlow(anyDouble(), anyDouble(), eq(18)))
				.thenThrow(new ProviderUnavailableException("HTTP_400", "no segment at 18"));
		when(trafficProvider.fetchFlow(anyDouble(), anyDouble(), eq(15)))
				.thenThrow(new ProviderUnavailableException("HTTP_400", "no segment at 15"));
		when(trafficProvider.fetchFlow(anyDouble(), anyDouble(), eq(12)))
				.thenReturn(nearReading(pilot, 40, 60, "FRC3"));

		mockMvc.perform(get("/api/destinations/{id}/traffic/external", pilot.getId()).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("MODERATE"))
				.andExpect(jsonPath("$.coverageAvailable").value(true));

		verify(trafficProvider, times(1)).fetchFlow(anyDouble(), anyDouble(), eq(18));
		verify(trafficProvider, times(1)).fetchFlow(anyDouble(), anyDouble(), eq(15));
		verify(trafficProvider, times(1)).fetchFlow(anyDouble(), anyDouble(), eq(12));
		verify(trafficProvider, never()).fetchFlow(anyDouble(), anyDouble(), eq(10));
	}

	@Test
	void distantGeometry_rejectedAsUnknown() throws Exception {
		Destination pilot = destinationRepository.findBySlugAndActiveTrue("india-gate").orElseThrow();
		when(trafficProvider.fetchFlow(anyDouble(), anyDouble(), anyInt()))
				.thenReturn(new FlowSegmentReading(
						30,
						60,
						0.9,
						false,
						"FRC3",
						List.of(new FlowSegmentCoordinate(1.0, 1.0))));

		mockMvc.perform(get("/api/destinations/{id}/traffic/external", pilot.getId()).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.coverageAvailable").value(false))
				.andExpect(jsonPath("$.message", containsString("No suitable nearby")));

		verify(trafficProvider, times(4)).fetchFlow(anyDouble(), anyDouble(), anyInt());
	}

	@Test
	void providerFailure_returnsUnavailable() throws Exception {
		when(trafficProvider.fetchFlow(anyDouble(), anyDouble(), anyInt()))
				.thenThrow(new ProviderUnavailableException("HTTP_500", "TomTom returned HTTP 500"));

		Long id = destinationRepository.findBySlugAndActiveTrue("india-gate").orElseThrow().getId();

		mockMvc.perform(get("/api/destinations/{id}/traffic/external", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNAVAILABLE"))
				.andExpect(jsonPath("$.message", containsString("500")));
	}

	@Test
	void communityTraffic_unchangedAndSeparate() throws Exception {
		Long id = destinationRepository.findBySlugAndActiveTrue("india-gate").orElseThrow().getId();

		mockMvc.perform(get("/api/destinations/{id}/traffic", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.source").value("USER_REPORTS"));

		verify(trafficProvider, never()).fetchFlow(anyDouble(), anyDouble(), anyInt());
	}

	@Test
	void nonPilot_returnsUnavailableWithoutCallingProvider() throws Exception {
		Long id = destinationRepository.findBySlugAndActiveTrue("taj-mahal").orElseThrow().getId();

		mockMvc.perform(get("/api/destinations/{id}/traffic/external", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNAVAILABLE"))
				.andExpect(jsonPath("$.pilotEnabled").value(false));

		verify(trafficProvider, never()).fetchFlow(anyDouble(), anyDouble(), anyInt());
	}

	@Test
	void providerStatus_exposesBudgetsAndRemaining() throws Exception {
		mockMvc.perform(get("/api/providers/traffic/status").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.provider").value("TOMTOM"))
				.andExpect(jsonPath("$.configured", equalTo(true)))
				.andExpect(jsonPath("$.scheduleEnabled", equalTo(false)))
				.andExpect(jsonPath("$.incidentsEnabled", equalTo(false)))
				.andExpect(jsonPath("$.cacheTtlMinutes").value(180))
				.andExpect(jsonPath("$.dailyRequestBudget").value(20))
				.andExpect(jsonPath("$.monthlyRequestBudget").value(15000))
				.andExpect(jsonPath("$.remainingDailyBudget").value(20))
				.andExpect(jsonPath("$.remainingMonthlyBudget").value(15000))
				.andExpect(jsonPath("$.flowZoomFallbacks[0]").value(18))
				.andExpect(jsonPath("$.pilotSlugs[0]").exists());
	}

	@Test
	void dailyBudget_blocksFurtherProviderCalls() throws Exception {
		Destination pilotDest = destinationRepository.findBySlugAndActiveTrue("india-gate").orElseThrow();
		Destination otherPilot = destinationRepository.findBySlugAndActiveTrue("amber-fort").orElseThrow();

		when(trafficProvider.fetchFlow(eq(pilotDest.getLatitude()), eq(pilotDest.getLongitude()), anyInt()))
				.thenReturn(nearReading(pilotDest, 50, 60, "FRC2"));
		when(trafficProvider.fetchFlow(eq(otherPilot.getLatitude()), eq(otherPilot.getLongitude()), anyInt()))
				.thenReturn(nearReading(otherPilot, 50, 60, "FRC2"));

		mockMvc.perform(get("/api/destinations/{id}/traffic/external", pilotDest.getId())).andExpect(status().isOk());

		LocalDate day = LocalDate.now(ZoneOffset.UTC);
		ProviderRequestStats stats = requestStatsRepository.findByProviderAndDayUtc("TOMTOM", day).orElseThrow();
		while (stats.getRequestCount() < 20) {
			stats.incrementRequest();
		}
		requestStatsRepository.save(stats);

		mockMvc.perform(get("/api/destinations/{id}/traffic/external", otherPilot.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNAVAILABLE"))
				.andExpect(jsonPath("$.message", containsString("budget")));

		verify(trafficProvider, times(1)).fetchFlow(anyDouble(), anyDouble(), anyInt());
	}

	@Test
	void expiredCache_withExhaustedBudget_returnsStaleWithoutProviderCall() throws Exception {
		Destination pilot = destinationRepository.findBySlugAndActiveTrue("india-gate").orElseThrow();
		Instant past = Instant.now().minusSeconds(7200);
		cache.put(new ExternalTrafficObservation(
				pilot.getId(),
				"TOMTOM",
				ExternalTrafficStatus.HEAVY,
				18,
				55,
				0.88,
				"FRC3",
				past,
				past.plusSeconds(60),
				"Traffic data © TomTom",
				null,
				true,
				ExternalTrafficObservation.SINGLE_ROAD_COVERAGE_NOTE,
				"stale observation"));

		LocalDate day = LocalDate.now(ZoneOffset.UTC);
		ProviderRequestStats stats = new ProviderRequestStats("TOMTOM", day);
		while (stats.getRequestCount() < 20) {
			stats.incrementRequest();
		}
		requestStatsRepository.save(stats);

		mockMvc.perform(get("/api/destinations/{id}/traffic/external", pilot.getId()).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("HEAVY"))
				.andExpect(jsonPath("$.currentSpeedKph").value(18))
				.andExpect(jsonPath("$.freshness").value("STALE"))
				.andExpect(jsonPath("$.coverageAvailable").value(true));

		verify(trafficProvider, never()).fetchFlow(anyDouble(), anyDouble(), anyInt());
	}

	@Test
	void concurrentBudgetConsumes_neverExceedDailyCap() throws Exception {
		int threads = 24;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger allowed = new AtomicInteger();
		try {
			List<Future<Boolean>> futures = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				futures.add(pool.submit(() -> {
					start.await(5, TimeUnit.SECONDS);
					return budgetService.tryConsume("TOMTOM");
				}));
			}
			start.countDown();
			for (Future<Boolean> future : futures) {
				if (Boolean.TRUE.equals(future.get(15, TimeUnit.SECONDS))) {
					allowed.incrementAndGet();
				}
			}
		}
		finally {
			pool.shutdownNow();
		}

		org.assertj.core.api.Assertions.assertThat(allowed.get()).isEqualTo(20);
		ProviderRequestStats stats = requestStatsRepository
				.findByProviderAndDayUtc("TOMTOM", LocalDate.now(ZoneOffset.UTC))
				.orElseThrow();
		org.assertj.core.api.Assertions.assertThat(stats.getRequestCount()).isEqualTo(20);
		org.assertj.core.api.Assertions.assertThat(stats.getBudgetSkipped()).isEqualTo(4);
	}

	private static FlowSegmentReading nearReading(Destination destination, int current, int freeFlow, String frc) {
		return new FlowSegmentReading(
				current,
				freeFlow,
				0.9,
				false,
				frc,
				List.of(new FlowSegmentCoordinate(destination.getLatitude(), destination.getLongitude())));
	}
}
