package com.pdcm.traffic;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.pdcm.common.Confidence;
import com.pdcm.report.CrowdObservation;
import com.pdcm.report.TrafficCondition;

class TrafficAggregatorTest {

	private final Instant now = Instant.parse("2026-11-06T12:00:00Z");

	@Test
	void unknownTrafficIsIgnored() {
		TrafficSummary summary = TrafficAggregator.summarize(1L, List.of(
				report(1, TrafficCondition.UNKNOWN),
				report(2, TrafficCondition.UNKNOWN)), now);

		assertThat(summary.status()).isEqualTo(TrafficCondition.UNKNOWN);
		assertThat(summary.confidence()).isEqualTo(Confidence.NONE);
		assertThat(summary.contributingReports()).isZero();
	}

	@Test
	void modeAndSeverityTieBreak() {
		TrafficSummary moderate = TrafficAggregator.summarize(1L, List.of(
				report(1, TrafficCondition.LIGHT),
				report(2, TrafficCondition.MODERATE),
				report(3, TrafficCondition.MODERATE)), now);
		assertThat(moderate.status()).isEqualTo(TrafficCondition.MODERATE);
		assertThat(moderate.confidence()).isEqualTo(Confidence.MEDIUM);

		TrafficSummary tie = TrafficAggregator.summarize(1L, List.of(
				report(1, TrafficCondition.LIGHT),
				report(2, TrafficCondition.HEAVY)), now);
		assertThat(tie.status()).isEqualTo(TrafficCondition.HEAVY);
		assertThat(tie.confidence()).isEqualTo(Confidence.MEDIUM);
	}

	@Test
	void fiveKnownReportsAreHighConfidence() {
		TrafficSummary summary = TrafficAggregator.summarize(1L, List.of(
				report(1, TrafficCondition.LIGHT),
				report(2, TrafficCondition.LIGHT),
				report(3, TrafficCondition.LIGHT),
				report(4, TrafficCondition.MODERATE),
				report(5, TrafficCondition.HEAVY)), now);

		assertThat(summary.status()).isEqualTo(TrafficCondition.LIGHT);
		assertThat(summary.confidence()).isEqualTo(Confidence.HIGH);
		assertThat(summary.contributingReports()).isEqualTo(5);
	}

	private static CrowdObservation report(long userId, TrafficCondition traffic) {
		return new CrowdObservation(userId, 5, traffic, Instant.parse("2026-11-06T12:00:00Z"));
	}
}
