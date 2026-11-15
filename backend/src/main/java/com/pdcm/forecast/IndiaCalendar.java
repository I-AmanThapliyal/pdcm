package com.pdcm.forecast;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

/**
 * Weekend, national holiday, and long-weekend facts for an India calendar date.
 * A long weekend is Friday through Monday when that Friday or Monday is a national holiday.
 * A midweek holiday stays a holiday and does not become a long weekend.
 */
public final class IndiaCalendar {

	public static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

	private IndiaCalendar() {
	}

	public static CalendarSignals signals(LocalDate date, Map<LocalDate, String> holidays) {
		DayOfWeek day = date.getDayOfWeek();
		boolean weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
		String holidayName = holidays.get(date);
		boolean longWeekend = false;
		if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY
				|| day == DayOfWeek.SUNDAY || day == DayOfWeek.MONDAY) {
			LocalDate friday = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
			LocalDate monday = friday.plusDays(3);
			longWeekend = holidays.containsKey(friday) || holidays.containsKey(monday);
		}
		return new CalendarSignals(weekend, holidayName != null, longWeekend, holidayName);
	}

	public static LocalDate nextHighlight(LocalDate today, Map<LocalDate, String> holidays) {
		for (int offset = 0; offset <= 60; offset++) {
			LocalDate candidate = today.plusDays(offset);
			if (signals(candidate, holidays).isLongWeekend()) {
				return candidate;
			}
		}
		return today.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
	}
}
