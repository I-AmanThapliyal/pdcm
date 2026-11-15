package com.pdcm.dashboard;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdcm.common.Confidence;
import com.pdcm.crowd.CrowdAggregator;
import com.pdcm.crowd.CrowdSummary;
import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationRepository;
import com.pdcm.destination.DestinationResponse;
import com.pdcm.externaltraffic.ExternalTrafficService;
import com.pdcm.forecast.CalendarSignals;
import com.pdcm.forecast.CrowdEstimate;
import com.pdcm.forecast.ForecastCalculator;
import com.pdcm.forecast.ForecastService;
import com.pdcm.forecast.HistoryPoint;
import com.pdcm.forecast.IndiaCalendar;
import com.pdcm.report.CrowdObservation;
import com.pdcm.report.RecentObservations;
import com.pdcm.report.ReportWindow;
import com.pdcm.report.VisitReport;
import com.pdcm.report.VisitReportRepository;
import com.pdcm.traffic.TrafficAggregator;
import com.pdcm.traffic.TrafficSummary;

@Service
@Transactional(readOnly = true)
public class DashboardService {

	private final DestinationRepository destinationRepository;
	private final VisitReportRepository visitReportRepository;
	private final RecentObservations recentObservations;
	private final ForecastService forecastService;
	private final ExternalTrafficService externalTrafficService;
	private final Clock clock;

	public DashboardService(
			DestinationRepository destinationRepository,
			VisitReportRepository visitReportRepository,
			RecentObservations recentObservations,
			ForecastService forecastService,
			ExternalTrafficService externalTrafficService,
			Clock clock) {
		this.destinationRepository = destinationRepository;
		this.visitReportRepository = visitReportRepository;
		this.recentObservations = recentObservations;
		this.forecastService = forecastService;
		this.externalTrafficService = externalTrafficService;
		this.clock = clock;
	}

	public DashboardResponse dashboard() {
		Instant asOf = clock.instant();
		Instant since = asOf.minus(Duration.ofDays(7));
		Map<Long, Long> counts = visitReportRepository.countByDestinationSince(since)
				.stream()
				.collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
		Map<Long, Long> verifiedCounts = visitReportRepository.countVerifiedByDestinationSince(since)
				.stream()
				.collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

		List<Destination> active = destinationRepository.findAllActive();
		Map<Long, List<CrowdObservation>> freshByDestination = recentObservations.allFresh(asOf);
		LocalDate tomorrow = LocalDate.now(IndiaCalendar.INDIA).plusDays(1);
		Map<Long, CrowdEstimate> forecasts = tomorrowForecasts(active, tomorrow, asOf);

		List<DestinationSnapshot> snapshots = active.stream()
				.map(destination -> snapshot(
						destination,
						counts.getOrDefault(destination.getId(), 0L),
						verifiedCounts.getOrDefault(destination.getId(), 0L),
						freshByDestination.getOrDefault(destination.getId(), List.of()),
						asOf,
						tomorrow,
						forecasts.getOrDefault(destination.getId(), CrowdEstimate.insufficient(0))))
				.toList();
		List<DestinationSnapshot> popular = snapshots.stream()
				.sorted(Comparator
						.comparingLong(DestinationSnapshot::recentReportCount).reversed()
						.thenComparing(item -> item.destination().name()))
				.limit(6)
				.toList();

		LocalDate today = LocalDate.now(IndiaCalendar.INDIA);
		Map<LocalDate, String> holidays = forecastService.holidaysBetween(today, today.plusDays(60));
		LocalDate highlightDate = IndiaCalendar.nextHighlight(today, holidays);
		CalendarSignals signals = IndiaCalendar.signals(highlightDate, holidays);

		List<RecentReportResponse> recent = visitReportRepository
				.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 8))
				.stream()
				.map(RecentReportResponse::from)
				.toList();

		// Quieter list uses live LOCATION_VERIFIED crowd only (confidence != NONE).
		// UNKNOWN / confidence NONE is never ranked as quiet.
		List<DestinationSnapshot> quieter = snapshots.stream()
				.filter(item -> item.crowd().confidence() != Confidence.NONE)
				.sorted(Comparator.comparing(item -> item.crowd().score()))
				.limit(3)
				.toList();

		return new DashboardResponse(
				asOf,
				ReportWindow.FRESHNESS.toHours(),
				catalogueCoverage(active),
				externalTrafficService.providerStatus(),
				popular,
				new CalendarHighlight(highlightDate, signals, signals.isLongWeekend() ? "LONG_WEEKEND" : "WEEKEND"),
				recent,
				quieter);
	}

	private Map<Long, CrowdEstimate> tomorrowForecasts(
			List<Destination> active,
			LocalDate tomorrow,
			Instant asOf) {
		Map<Long, List<HistoryPoint>> historyByDestination = new HashMap<>();
		Instant cutoff = asOf.minus(ForecastCalculator.LOOKBACK);
		for (VisitReport report : visitReportRepository.findAllHistorySince(cutoff)) {
			historyByDestination
					.computeIfAbsent(report.getDestination().getId(), ignored -> new ArrayList<>())
					.add(new HistoryPoint(
							report.getUser().getId(),
							report.getObservedAt(),
							report.getCrowdIntensity()));
		}
		Map<Long, CrowdEstimate> forecasts = new HashMap<>();
		for (Destination destination : active) {
			List<HistoryPoint> history = historyByDestination.getOrDefault(destination.getId(), List.of());
			forecasts.put(destination.getId(), ForecastCalculator.estimate(tomorrow, history, asOf));
		}
		return forecasts;
	}

	private static CatalogueCoverage catalogueCoverage(List<Destination> active) {
		List<String> regions = active.stream()
				.map(Destination::getRegion)
				.distinct()
				.sorted()
				.toList();
		return new CatalogueCoverage(active.size(), regions.size(), regions);
	}

	private static DestinationSnapshot snapshot(
			Destination destination,
			long reportCount,
			long verifiedReportCount,
			List<CrowdObservation> freshReports,
			Instant asOf,
			LocalDate tomorrow,
			CrowdEstimate estimate) {
		CrowdSummary crowd = CrowdAggregator.summarize(destination.getId(), freshReports, asOf);
		TrafficSummary traffic = TrafficAggregator.summarize(destination.getId(), freshReports, asOf);
		return new DestinationSnapshot(
				DestinationResponse.from(destination),
				crowd,
				traffic,
				reportCount,
				verifiedReportCount,
				DashboardForecastQuality.from(tomorrow, estimate));
	}
}
