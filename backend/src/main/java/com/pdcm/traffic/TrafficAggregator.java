package com.pdcm.traffic;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.pdcm.common.Confidence;
import com.pdcm.report.CrowdObservation;
import com.pdcm.report.ReportWindow;
import com.pdcm.report.TrafficCondition;

/**
 * Destination traffic from the same 6-hour latest-per-user window as crowd,
 * using mode and a severity tie-break. Crowd intensity is ignored.
 */
public final class TrafficAggregator {

	private TrafficAggregator() {
	}

	public static TrafficSummary summarize(Long destinationId, List<CrowdObservation> raw, Instant now) {
		List<CrowdObservation> known = ReportWindow.latestPerUser(raw, now).stream()
				.filter(report -> report.traffic() != TrafficCondition.UNKNOWN)
				.toList();
		if (known.isEmpty()) {
			return TrafficSummary.unknown(destinationId, now);
		}

		Map<TrafficCondition, Integer> counts = new EnumMap<>(TrafficCondition.class);
		for (CrowdObservation report : known) {
			counts.merge(report.traffic(), 1, Integer::sum);
		}
		TrafficCondition status = counts.entrySet().stream()
				.max((left, right) -> {
					int byCount = Integer.compare(left.getValue(), right.getValue());
					if (byCount != 0) {
						return byCount;
					}
					return Integer.compare(severity(left.getKey()), severity(right.getKey()));
				})
				.map(Map.Entry::getKey)
				.orElse(TrafficCondition.UNKNOWN);

		return new TrafficSummary(
				destinationId,
				status,
				confidence(known.size()),
				known.size(),
				ReportWindow.latestObserved(known),
				now,
				"USER_REPORTS");
	}

	static int severity(TrafficCondition condition) {
		return switch (condition) {
			case LIGHT -> 1;
			case MODERATE -> 2;
			case HEAVY -> 3;
			case UNKNOWN -> 0;
		};
	}

	static Confidence confidence(int count) {
		if (count <= 0) {
			return Confidence.NONE;
		}
		if (count == 1) {
			return Confidence.LOW;
		}
		if (count <= 4) {
			return Confidence.MEDIUM;
		}
		return Confidence.HIGH;
	}
}
