package com.pdcm.externaltraffic;

import java.time.Instant;
import java.util.List;

public record ProviderStatusResponse(
		String provider,
		ProviderHealthStatus status,
		boolean configured,
		boolean scheduleEnabled,
		boolean incidentsEnabled,
		int cacheTtlMinutes,
		int dailyRequestBudget,
		int monthlyRequestBudget,
		int requestsToday,
		int requestsThisMonth,
		int remainingDailyBudget,
		int remainingMonthlyBudget,
		int successesToday,
		int failuresToday,
		int budgetSkippedToday,
		Instant lastSuccessAt,
		Instant lastFailureAt,
		String lastErrorCode,
		int consecutiveFailures,
		List<String> pilotSlugs,
		List<Integer> flowZoomFallbacks,
		String usageNote
) {
}
