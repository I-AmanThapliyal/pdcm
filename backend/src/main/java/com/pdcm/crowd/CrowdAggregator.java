package com.pdcm.crowd;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.pdcm.common.Confidence;
import com.pdcm.report.CrowdObservation;
import com.pdcm.report.ReportWindow;

/**
 * Deterministic current-crowd score. Traffic fields are ignored.
 */
public final class CrowdAggregator {

	private static final BigDecimal FULL = new BigDecimal("1.0");
	private static final BigDecimal MID = new BigDecimal("0.6");
	private static final BigDecimal LOW = new BigDecimal("0.3");

	private CrowdAggregator() {
	}

	public static CrowdSummary summarize(Long destinationId, List<CrowdObservation> raw, Instant now) {
		List<CrowdObservation> reports = ReportWindow.latestPerUser(raw, now);
		if (reports.isEmpty()) {
			return CrowdSummary.unknown(destinationId, now);
		}

		BigDecimal weighted = BigDecimal.ZERO;
		BigDecimal weightSum = BigDecimal.ZERO;
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (CrowdObservation report : reports) {
			BigDecimal weight = weightFor(ReportWindow.effectiveAge(report.observedAt(), now));
			weighted = weighted.add(BigDecimal.valueOf(report.crowdIntensity()).multiply(weight));
			weightSum = weightSum.add(weight);
			min = Math.min(min, report.crowdIntensity());
			max = Math.max(max, report.crowdIntensity());
		}

		BigDecimal score = weighted.divide(weightSum, 1, RoundingMode.HALF_UP);
		boolean agreeing = (max - min) <= 1;
		return new CrowdSummary(
				destinationId,
				statusFor(score),
				score,
				confidence(reports.size(), agreeing),
				reports.size(),
				ReportWindow.latestObserved(reports),
				now,
				"USER_REPORTS");
	}

	static BigDecimal weightFor(Duration age) {
		if (age.compareTo(Duration.ofHours(1)) < 0) {
			return FULL;
		}
		if (age.compareTo(Duration.ofHours(3)) < 0) {
			return MID;
		}
		return LOW;
	}

	public static CrowdStatus statusFor(BigDecimal score) {
		if (score.compareTo(new BigDecimal("1.6")) < 0) {
			return CrowdStatus.QUIET;
		}
		if (score.compareTo(new BigDecimal("2.6")) < 0) {
			return CrowdStatus.MODERATE;
		}
		if (score.compareTo(new BigDecimal("3.6")) < 0) {
			return CrowdStatus.BUSY;
		}
		if (score.compareTo(new BigDecimal("4.6")) < 0) {
			return CrowdStatus.CROWDED;
		}
		return CrowdStatus.OVERCROWDED;
	}

	static Confidence confidence(int count, boolean agreeing) {
		if (count <= 0) {
			return Confidence.NONE;
		}
		if (count == 1) {
			return Confidence.LOW;
		}
		if (count <= 4) {
			return agreeing ? Confidence.MEDIUM : Confidence.LOW;
		}
		return agreeing ? Confidence.HIGH : Confidence.MEDIUM;
	}
}
