package com.pdcm.forecast;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdcm.common.NotFoundException;
import com.pdcm.destination.DestinationRepository;
import com.pdcm.report.VisitReportRepository;

@Service
@Transactional(readOnly = true)
public class ForecastService {

	private final DestinationRepository destinationRepository;
	private final VisitReportRepository visitReportRepository;
	private final HolidayRepository holidayRepository;
	private final Clock clock;

	public ForecastService(
			DestinationRepository destinationRepository,
			VisitReportRepository visitReportRepository,
			HolidayRepository holidayRepository,
			Clock clock) {
		this.destinationRepository = destinationRepository;
		this.visitReportRepository = visitReportRepository;
		this.holidayRepository = holidayRepository;
		this.clock = clock;
	}

	public ForecastResponse forecast(Long destinationId, LocalDate requestedDate) {
		destinationRepository.findByIdAndActiveTrue(destinationId)
				.orElseThrow(() -> new NotFoundException("Destination not found: " + destinationId));
		Instant asOf = clock.instant();
		LocalDate target = requestedDate != null
				? requestedDate
				: LocalDate.now(IndiaCalendar.INDIA).plusDays(1);
		Map<LocalDate, String> holidays = holidaysAround(target);
		List<HistoryPoint> history = visitReportRepository
				.findHistory(destinationId, asOf.minus(ForecastCalculator.LOOKBACK))
				.stream()
				.map(report -> new HistoryPoint(
						report.getUser().getId(),
						report.getObservedAt(),
						report.getCrowdIntensity()))
				.toList();
		return new ForecastResponse(
				destinationId,
				target,
				IndiaCalendar.signals(target, holidays),
				ForecastCalculator.estimate(target, history, asOf),
				asOf);
	}

	public Map<LocalDate, String> holidaysBetween(LocalDate start, LocalDate end) {
		return holidayRepository.findByHolidayDateBetween(start, end).stream()
				.collect(Collectors.toMap(Holiday::getHolidayDate, Holiday::getName));
	}

	private Map<LocalDate, String> holidaysAround(LocalDate target) {
		return holidaysBetween(target.minusDays(7), target.plusDays(7));
	}
}
