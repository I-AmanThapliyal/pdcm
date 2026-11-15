package com.pdcm.dashboard;

import java.time.Instant;
import java.util.List;

import com.pdcm.externaltraffic.ProviderStatusResponse;

/**
 * Public dashboard payload. Crowd and community traffic stay separate channels;
 * {@code externalTrafficProvider} is display-only health (no Flow fetch on this path).
 */
public record DashboardResponse(
		Instant asOf,
		long observationWindowHours,
		CatalogueCoverage catalogueCoverage,
		ProviderStatusResponse externalTrafficProvider,
		List<DestinationSnapshot> popularDestinations,
		CalendarHighlight calendarHighlight,
		List<RecentReportResponse> recentReports,
		List<DestinationSnapshot> quieterKnownDestinations
) {
}
