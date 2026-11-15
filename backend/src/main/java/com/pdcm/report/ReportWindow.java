package com.pdcm.report;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Selects the latest report per user whose observation is still inside the 6-hour freshness window.
 * Age is {@code max(0, now - observedAt)} so a small future skew counts as age zero and is included.
 */
public final class ReportWindow {

	public static final Duration FRESHNESS = Duration.ofHours(6);

	private ReportWindow() {
	}

	public static List<CrowdObservation> latestPerUser(List<CrowdObservation> reports, Instant now) {
		Map<Long, CrowdObservation> latest = new HashMap<>();
		for (CrowdObservation report : reports) {
			if (!isFresh(report.observedAt(), now)) {
				continue;
			}
			latest.merge(report.userId(), report, ReportWindow::newer);
		}
		return new ArrayList<>(latest.values());
	}

	public static boolean isFresh(Instant observedAt, Instant now) {
		Duration age = Duration.between(observedAt, now);
		if (age.isNegative()) {
			return true;
		}
		return age.compareTo(FRESHNESS) < 0;
	}

	public static Duration effectiveAge(Instant observedAt, Instant now) {
		Duration age = Duration.between(observedAt, now);
		return age.isNegative() ? Duration.ZERO : age;
	}

	private static CrowdObservation newer(CrowdObservation left, CrowdObservation right) {
		int byObserved = left.observedAt().compareTo(right.observedAt());
		return byObserved >= 0 ? left : right;
	}

	public static Instant latestObserved(List<CrowdObservation> reports) {
		return reports.stream()
				.map(CrowdObservation::observedAt)
				.max(Comparator.naturalOrder())
				.orElse(null);
	}
}
