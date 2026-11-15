package com.pdcm.recommendation;

import java.math.BigDecimal;
import java.time.Instant;

import com.pdcm.common.Confidence;
import com.pdcm.crowd.CrowdStatus;
import com.pdcm.destination.DestinationResponse;

/**
 * Nearby alternative. {@code informationQuality=INSUFFICIENT_DATA} / UNKNOWN crowd
 * must never be presented as confirmed quiet. Freshness fields come from live crowd only.
 */
public record AlternativeResponse(
		DestinationResponse destination,
		double distanceKm,
		boolean sameCategory,
		CrowdStatus crowdStatus,
		BigDecimal crowdScore,
		Confidence crowdConfidence,
		String informationQuality,
		int contributingReports,
		Instant lastObservedAt,
		String source
) {
}
