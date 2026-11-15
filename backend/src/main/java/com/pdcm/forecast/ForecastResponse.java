package com.pdcm.forecast;

import java.time.Instant;
import java.time.LocalDate;

public record ForecastResponse(
		Long destinationId,
		LocalDate targetDate,
		CalendarSignals calendarSignals,
		CrowdEstimate crowdEstimate,
		Instant asOf
) {
}
