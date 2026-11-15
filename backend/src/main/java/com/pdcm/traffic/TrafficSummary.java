package com.pdcm.traffic;

import java.time.Instant;

import com.pdcm.common.Confidence;
import com.pdcm.report.TrafficCondition;

public record TrafficSummary(
		Long destinationId,
		TrafficCondition status,
		Confidence confidence,
		int contributingReports,
		Instant lastObservedAt,
		Instant asOf,
		String source
) {
	public static TrafficSummary unknown(Long destinationId, Instant asOf) {
		return new TrafficSummary(
				destinationId,
				TrafficCondition.UNKNOWN,
				Confidence.NONE,
				0,
				null,
				asOf,
				"USER_REPORTS");
	}
}
