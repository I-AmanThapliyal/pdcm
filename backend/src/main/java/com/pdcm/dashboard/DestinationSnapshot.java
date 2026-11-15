package com.pdcm.dashboard;

import com.pdcm.crowd.CrowdSummary;
import com.pdcm.destination.DestinationResponse;
import com.pdcm.traffic.TrafficSummary;

/**
 * Dashboard row. {@code recentReportCount} is activity/popularity (all statuses).
 * {@code verifiedRecentReportCount} is LOCATION_VERIFIED only — not the same as live crowd.
 * Live crowd/traffic come from the 6-hour LOCATION_VERIFIED aggregation on {@code crowd}/{@code traffic}.
 * {@code forecastQuality} is a historical estimate for tomorrow — never a live observation.
 * Community traffic is separate from external road provider status on the dashboard envelope.
 */
public record DestinationSnapshot(
		DestinationResponse destination,
		CrowdSummary crowd,
		TrafficSummary traffic,
		long recentReportCount,
		long verifiedRecentReportCount,
		DashboardForecastQuality forecastQuality
) {
}
