package com.pdcm.forecast;

import java.math.BigDecimal;

import com.pdcm.common.Confidence;
import com.pdcm.crowd.CrowdStatus;

public record CrowdEstimate(
		ForecastStatus forecastStatus,
		BigDecimal estimatedIntensity,
		CrowdStatus estimatedStatus,
		Confidence confidence,
		String label,
		int sampleSize,
		String message
) {
	public static CrowdEstimate insufficient(int sampleSize) {
		return new CrowdEstimate(
				ForecastStatus.INSUFFICIENT_DATA,
				null,
				null,
				Confidence.NONE,
				"ESTIMATE_UNAVAILABLE",
				sampleSize,
				"Not enough same-weekday historical reports (need 5 in 90 days).");
	}
}
