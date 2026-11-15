package com.pdcm.forecast;

public record CalendarSignals(
		boolean isWeekend,
		boolean isNationalHoliday,
		boolean isLongWeekend,
		String holidayName
) {
}
