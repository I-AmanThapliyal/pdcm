package com.pdcm.crowd;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.pdcm.common.Confidence;
import com.pdcm.report.CrowdObservation;
import com.pdcm.report.TrafficCondition;

class CrowdAggregatorTest {

	private final Instant now = Instant.parse("2026-11-06T12:00:00Z");

	@Test
	void noReports_isUnknown() {
		CrowdSummary summary = CrowdAggregator.summarize(1L, List.of(), now);

		assertThat(summary.status()).isEqualTo(CrowdStatus.UNKNOWN);
		assertThat(summary.score()).isNull();
		assertThat(summary.confidence()).isEqualTo(Confidence.NONE);
		assertThat(summary.contributingReports()).isZero();
	}

	@Test
	void expiredAndFutureSkew() {
		CrowdObservation expired = obs(1, 5, now.minus(Duration.ofHours(6)));
		CrowdObservation freshFuture = obs(2, 2, now.plusSeconds(30));

		CrowdSummary expiredOnly = CrowdAggregator.summarize(1L, List.of(expired), now);
		assertThat(expiredOnly.status()).isEqualTo(CrowdStatus.UNKNOWN);

		CrowdSummary future = CrowdAggregator.summarize(1L, List.of(freshFuture), now);
		assertThat(future.score()).isEqualByComparingTo("2.0");
		assertThat(future.status()).isEqualTo(CrowdStatus.MODERATE);
		assertThat(future.confidence()).isEqualTo(Confidence.LOW);
	}

	@Test
	void weightsBoundariesAndDisagreement() {
		CrowdObservation full = obs(1, 5, now.minus(Duration.ofMinutes(30)));
		CrowdObservation mid = obs(2, 1, now.minus(Duration.ofHours(1)));
		CrowdObservation low = obs(3, 1, now.minus(Duration.ofHours(3)));

		assertThat(CrowdAggregator.weightFor(Duration.ofMinutes(59))).isEqualByComparingTo("1.0");
		assertThat(CrowdAggregator.weightFor(Duration.ofHours(1))).isEqualByComparingTo("0.6");
		assertThat(CrowdAggregator.weightFor(Duration.ofHours(3))).isEqualByComparingTo("0.3");

		CrowdSummary two = CrowdAggregator.summarize(1L, List.of(full, mid), now);
		assertThat(two.score()).isEqualByComparingTo("3.5");
		assertThat(two.status()).isEqualTo(CrowdStatus.BUSY);
		assertThat(two.confidence()).isEqualTo(Confidence.LOW);

		CrowdSummary withLow = CrowdAggregator.summarize(1L, List.of(full, low), now);
		assertThat(withLow.score()).isEqualByComparingTo(new BigDecimal("4.1"));
		assertThat(withLow.status()).isEqualTo(CrowdStatus.CROWDED);
	}

	@Test
	void sameUserCannotInflateTheSample() {
		CrowdObservation older = obs(7, 1, now.minus(Duration.ofMinutes(20)));
		CrowdObservation newer = obs(7, 5, now.minus(Duration.ofMinutes(5)));

		CrowdSummary summary = CrowdAggregator.summarize(1L, List.of(older, newer), now);

		assertThat(summary.contributingReports()).isEqualTo(1);
		assertThat(summary.score()).isEqualByComparingTo("5.0");
		assertThat(summary.status()).isEqualTo(CrowdStatus.OVERCROWDED);
	}

	@Test
	void fiveAgreeingReportsAreHighConfidence() {
		List<CrowdObservation> reports = List.of(
				obs(1, 3, now),
				obs(2, 3, now.minusSeconds(10)),
				obs(3, 4, now.minusSeconds(20)),
				obs(4, 3, now.minusSeconds(30)),
				obs(5, 3, now.minusSeconds(40)));

		CrowdSummary summary = CrowdAggregator.summarize(1L, reports, now);

		assertThat(summary.score()).isEqualByComparingTo("3.2");
		assertThat(summary.status()).isEqualTo(CrowdStatus.BUSY);
		assertThat(summary.confidence()).isEqualTo(Confidence.HIGH);
	}

	@Test
	void trafficDoesNotChangeCrowdScore() {
		CrowdObservation light = new CrowdObservation(1, 4, TrafficCondition.LIGHT, now);
		CrowdObservation heavy = new CrowdObservation(1, 4, TrafficCondition.HEAVY, now);

		assertThat(CrowdAggregator.summarize(1L, List.of(light), now).score())
				.isEqualByComparingTo(CrowdAggregator.summarize(1L, List.of(heavy), now).score());
	}

	private static CrowdObservation obs(long userId, int intensity, Instant observedAt) {
		return new CrowdObservation(userId, intensity, TrafficCondition.UNKNOWN, observedAt);
	}
}
