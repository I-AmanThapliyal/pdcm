package com.pdcm.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.pdcm.common.Confidence;
import com.pdcm.crowd.CrowdStatus;
import com.pdcm.forecast.CrowdEstimate;
import com.pdcm.forecast.ForecastStatus;

/**
 * Honest tomorrow forecast snapshot for the dashboard.
 * {@link ForecastStatus#INSUFFICIENT_DATA} never invents a numeric intensity.
 * Calendar facts stay on {@link CalendarHighlight}, not here.
 */
public record DashboardForecastQuality(
		LocalDate targetDate,
		ForecastStatus forecastStatus,
		BigDecimal estimatedIntensity,
		CrowdStatus estimatedStatus,
		Confidence confidence,
		int sampleSize,
		String message
) {
	public static DashboardForecastQuality from(LocalDate targetDate, CrowdEstimate estimate) {
		return new DashboardForecastQuality(
				targetDate,
				estimate.forecastStatus(),
				estimate.estimatedIntensity(),
				estimate.estimatedStatus(),
				estimate.confidence(),
				estimate.sampleSize(),
				estimate.message());
	}
}
