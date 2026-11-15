package com.pdcm.crowd;

import java.math.BigDecimal;
import java.time.Instant;

import com.pdcm.common.Confidence;

public record CrowdSummary(
		Long destinationId,
		CrowdStatus status,
		BigDecimal score,
		Confidence confidence,
		int contributingReports,
		Instant lastObservedAt,
		Instant asOf,
		String source
) {
	public static CrowdSummary unknown(Long destinationId, Instant asOf) {
		return new CrowdSummary(
				destinationId,
				CrowdStatus.UNKNOWN,
				null,
				Confidence.NONE,
				0,
				null,
				asOf,
				"USER_REPORTS");
	}
}
