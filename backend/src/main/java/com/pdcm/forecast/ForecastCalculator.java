package com.pdcm.forecast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pdcm.common.Confidence;
import com.pdcm.crowd.CrowdAggregator;

/**
 * Same-weekday historical estimate. Does not invent samples and does not use calendar multipliers.
 */
public final class ForecastCalculator {

	public static final int MINIMUM_SAMPLES = 5;
	public static final Duration LOOKBACK = Duration.ofDays(90);

	private ForecastCalculator() {
	}

	public static CrowdEstimate estimate(LocalDate targetDate, List<HistoryPoint> reports, Instant asOf) {
		Instant cutoff = asOf.minus(LOOKBACK);
		Map<SampleKey, HistoryPoint> deduped = new HashMap<>();
		for (HistoryPoint report : reports) {
			if (report.observedAt().isBefore(cutoff)) {
				continue;
			}
			LocalDate indiaDate = report.observedAt().atZone(IndiaCalendar.INDIA).toLocalDate();
			if (indiaDate.getDayOfWeek() != targetDate.getDayOfWeek()) {
				continue;
			}
			SampleKey key = new SampleKey(report.userId(), indiaDate);
			deduped.merge(key, report, (left, right) ->
					left.observedAt().isAfter(right.observedAt()) ? left : right);
		}

		if (deduped.size() < MINIMUM_SAMPLES) {
			return CrowdEstimate.insufficient(deduped.size());
		}

		BigDecimal sum = deduped.values().stream()
				.map(point -> BigDecimal.valueOf(point.intensity()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal mean = sum.divide(BigDecimal.valueOf(deduped.size()), 1, RoundingMode.HALF_UP);
		Confidence confidence = deduped.size() >= 10 ? Confidence.MEDIUM : Confidence.LOW;
		return new CrowdEstimate(
				ForecastStatus.ESTIMATE,
				mean,
				CrowdAggregator.statusFor(mean),
				confidence,
				"ESTIMATE",
				deduped.size(),
				"Estimate from same-weekday historical reports. Not a live observation.");
	}

	private record SampleKey(long userId, LocalDate indiaDate) {
	}
}
