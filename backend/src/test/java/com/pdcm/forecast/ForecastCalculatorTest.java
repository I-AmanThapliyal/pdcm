package com.pdcm.forecast;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.pdcm.common.Confidence;
import com.pdcm.crowd.CrowdStatus;

class ForecastCalculatorTest {

	private final Instant asOf = Instant.parse("2026-11-06T12:00:00Z");
	private final LocalDate saturday = LocalDate.of(2026, 11, 7);

	@Test
	void fewerThanFiveSamples_isInsufficient() {
		CrowdEstimate estimate = ForecastCalculator.estimate(saturday, List.of(
				point(1, saturday.minusWeeks(1), 4),
				point(2, saturday.minusWeeks(2), 4)), asOf);

		assertThat(estimate.forecastStatus()).isEqualTo(ForecastStatus.INSUFFICIENT_DATA);
		assertThat(estimate.estimatedIntensity()).isNull();
		assertThat(estimate.sampleSize()).isEqualTo(2);
		assertThat(estimate.confidence()).isEqualTo(Confidence.NONE);
		assertThat(estimate.label()).isEqualTo("ESTIMATE_UNAVAILABLE");
	}

	@Test
	void fiveSameWeekdaySamples_areAnEstimate() {
		List<HistoryPoint> points = new ArrayList<>();
		for (int week = 1; week <= 5; week++) {
			points.add(point(week, saturday.minusWeeks(week), 2));
		}
		points.add(point(1, saturday.minusWeeks(1), 5));

		CrowdEstimate estimate = ForecastCalculator.estimate(saturday, points, asOf);

		assertThat(estimate.forecastStatus()).isEqualTo(ForecastStatus.ESTIMATE);
		assertThat(estimate.sampleSize()).isEqualTo(5);
		assertThat(estimate.estimatedIntensity()).isEqualByComparingTo("2.6");
		assertThat(estimate.estimatedStatus()).isEqualTo(CrowdStatus.BUSY);
		assertThat(estimate.confidence()).isEqualTo(Confidence.LOW);
		assertThat(estimate.label()).isEqualTo("ESTIMATE");
	}

	@Test
	void otherWeekdayAndOldReports_doNotCount() {
		HistoryPoint wrongDay = point(1, saturday.minusDays(1), 5);
		HistoryPoint tooOld = point(2, saturday.minusWeeks(20), 5);

		CrowdEstimate estimate = ForecastCalculator.estimate(saturday, List.of(wrongDay, tooOld), asOf);

		assertThat(estimate.sampleSize()).isZero();
	}

	@Test
	void fridayHoliday_marksTheWeekendSpan() {
		Map<LocalDate, String> holidays = Map.of(LocalDate.of(2026, 12, 25), "Christmas");

		assertThat(IndiaCalendar.signals(LocalDate.of(2026, 12, 25), holidays).isLongWeekend()).isTrue();
		assertThat(IndiaCalendar.signals(LocalDate.of(2026, 12, 27), holidays).isLongWeekend()).isTrue();
		assertThat(IndiaCalendar.signals(LocalDate.of(2026, 12, 28), holidays).isLongWeekend()).isTrue();
		assertThat(IndiaCalendar.signals(LocalDate.of(2026, 12, 24), holidays).isLongWeekend()).isFalse();
		assertThat(IndiaCalendar.signals(LocalDate.of(2026, 11, 7), holidays).isWeekend()).isTrue();
		assertThat(IndiaCalendar.signals(LocalDate.of(2026, 11, 7), holidays).isNationalHoliday()).isFalse();
	}

	private static HistoryPoint point(long userId, LocalDate indiaDate, int intensity) {
		return new HistoryPoint(userId, indiaDate.atTime(10, 0).atZone(IndiaCalendar.INDIA).toInstant(), intensity);
	}
}
