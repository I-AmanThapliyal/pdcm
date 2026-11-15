package com.pdcm.dashboard;

import java.time.LocalDate;

import com.pdcm.forecast.CalendarSignals;

public record CalendarHighlight(
		LocalDate targetDate,
		CalendarSignals calendarSignals,
		String label
) {
}
