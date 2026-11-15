package com.pdcm.externaltraffic;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.pdcm.support.PostgresTestcontainers;

/**
 * Confirms daily/monthly budget buckets use UTC calendar boundaries, not the host zone.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProviderBudgetUtcBoundaryTest {

	@Container
	static PostgreSQLContainer<?> postgres = PostgresTestcontainers.create();

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry registry) {
		PostgresTestcontainers.register(registry, postgres);
		registry.add("pdcm.external-traffic.daily-request-budget", () -> "20");
		registry.add("pdcm.external-traffic.monthly-request-budget", () -> "10");
	}

	@TestConfiguration
	static class FixedClockConfig {
		/** Just before UTC midnight → still previous UTC day. */
		static final Instant BEFORE_UTC_MIDNIGHT = Instant.parse("2026-03-01T23:59:30Z");
		/** Just after UTC midnight → new UTC day (still March). */
		static final Instant AFTER_UTC_MIDNIGHT = Instant.parse("2026-03-02T00:00:30Z");
		/** First instant of April UTC → new month bucket. */
		static final Instant APRIL_UTC = Instant.parse("2026-04-01T00:00:30Z");

		static final MutableClock CLOCK = new MutableClock(BEFORE_UTC_MIDNIGHT);

		@Bean
		@Primary
		Clock testClock() {
			return CLOCK;
		}
	}

	static final class MutableClock extends Clock {
		private volatile Instant instant;

		MutableClock(Instant instant) {
			this.instant = instant;
		}

		void set(Instant instant) {
			this.instant = instant;
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return Clock.fixed(instant, zone);
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}

	@Autowired
	ProviderBudgetService budgetService;

	@Autowired
	ProviderRequestStatsRepository requestStatsRepository;

	@BeforeEach
	void clean() {
		requestStatsRepository.deleteAll();
		FixedClockConfig.CLOCK.set(FixedClockConfig.BEFORE_UTC_MIDNIGHT);
	}

	@Test
	void utcDayBoundary_usesSeparateDailyBuckets() {
		assertThat(budgetService.utcDay()).isEqualTo(LocalDate.of(2026, 3, 1));
		assertThat(budgetService.tryConsume("TOMTOM")).isTrue();

		FixedClockConfig.CLOCK.set(FixedClockConfig.AFTER_UTC_MIDNIGHT);
		assertThat(budgetService.utcDay()).isEqualTo(LocalDate.of(2026, 3, 2));
		assertThat(budgetService.tryConsume("TOMTOM")).isTrue();

		assertThat(requestStatsRepository.findByProviderAndDayUtc("TOMTOM", LocalDate.of(2026, 3, 1)))
				.get()
				.extracting(ProviderRequestStats::getRequestCount)
				.isEqualTo(1);
		assertThat(requestStatsRepository.findByProviderAndDayUtc("TOMTOM", LocalDate.of(2026, 3, 2)))
				.get()
				.extracting(ProviderRequestStats::getRequestCount)
				.isEqualTo(1);
		assertThat(budgetService.requestsThisMonth("TOMTOM")).isEqualTo(2);
	}

	@Test
	void utcMonthBoundary_resetsMonthlyRemaining() {
		FixedClockConfig.CLOCK.set(Instant.parse("2026-03-31T12:00:00Z"));
		for (int i = 0; i < 10; i++) {
			assertThat(budgetService.tryConsume("TOMTOM")).isTrue();
		}
		assertThat(budgetService.tryConsume("TOMTOM")).isFalse();
		assertThat(budgetService.remainingMonthly("TOMTOM")).isZero();

		FixedClockConfig.CLOCK.set(FixedClockConfig.APRIL_UTC);
		assertThat(budgetService.utcDay()).isEqualTo(LocalDate.of(2026, 4, 1));
		assertThat(budgetService.remainingMonthly("TOMTOM")).isEqualTo(10);
		assertThat(budgetService.tryConsume("TOMTOM")).isTrue();
		assertThat(budgetService.requestsThisMonth("TOMTOM")).isEqualTo(1);
	}
}
