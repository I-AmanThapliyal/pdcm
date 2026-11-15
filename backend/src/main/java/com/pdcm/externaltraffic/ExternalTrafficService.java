package com.pdcm.externaltraffic;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdcm.common.NotFoundException;
import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationRepository;

@Service
public class ExternalTrafficService {

	private final DestinationRepository destinationRepository;
	private final TrafficProvider trafficProvider;
	private final ExternalTrafficProperties properties;
	private final ExternalTrafficCache cache;
	private final ProviderBudgetService budgetService;
	private final ProviderHealthTracker healthTracker;
	private final Clock clock;
	/** Per-destination single-flight locks — concurrent opens share one provider fetch. */
	private final ConcurrentHashMap<Long, Object> flightLocks = new ConcurrentHashMap<>();

	public ExternalTrafficService(
			DestinationRepository destinationRepository,
			TrafficProvider trafficProvider,
			ExternalTrafficProperties properties,
			ExternalTrafficCache cache,
			ProviderBudgetService budgetService,
			ProviderHealthTracker healthTracker,
			Clock clock) {
		this.destinationRepository = destinationRepository;
		this.trafficProvider = trafficProvider;
		this.properties = properties;
		this.cache = cache;
		this.budgetService = budgetService;
		this.healthTracker = healthTracker;
		this.clock = clock;
	}

	@Transactional
	public ExternalTrafficResponse current(Long destinationId) {
		Destination destination = destinationRepository.findByIdAndActiveTrue(destinationId)
				.orElseThrow(() -> new NotFoundException("Destination not found: " + destinationId));
		boolean pilot = isPilot(destination.getSlug());
		healthTracker.markConfigured(trafficProvider.providerId(), properties.hasApiKey());

		if (!properties.isEnabled() || !properties.hasApiKey()) {
			return ExternalTrafficResponse.from(unavailable(
					destinationId,
					"External traffic provider is not configured. Set PDCM_TOMTOM_API_KEY to enable the pilot."),
					pilot);
		}
		if (!pilot) {
			return ExternalTrafficResponse.from(unavailable(
					destinationId,
					"External road traffic pilot is limited to selected destinations."),
					false);
		}

		Instant now = clock.instant();
		return cache.getFresh(destinationId, now)
				.map(observation -> ExternalTrafficResponse.from(observation, true))
				.orElseGet(() -> fetchUnderLock(destination));
	}

	@Transactional
	public void refreshPilotDestination(Destination destination) {
		if (!isPilot(destination.getSlug())) {
			return;
		}
		Object lock = flightLocks.computeIfAbsent(destination.getId(), id -> new Object());
		synchronized (lock) {
			fetchAndCache(destination, clock.instant());
		}
	}

	@Transactional(readOnly = true)
	public ProviderStatusResponse providerStatus() {
		String provider = trafficProvider.providerId();
		ProviderHealth health = healthTracker.read(provider);
		ProviderRequestStats stats = budgetService.snapshot(provider);
		int requestsMonth = budgetService.requestsThisMonth(provider);
		boolean configured = properties.hasApiKey();
		String usageNote = "On-demand only with "
				+ properties.getCacheTtlMinutes()
				+ "m shared destination in-memory cache. Scheduled refresh is "
				+ (properties.isScheduleEnabled() ? "ON" : "OFF (required)")
				+ ". Flow zoom fallback "
				+ properties.getFlowZoomFallbacks()
				+ " (each attempt counts). Daily hard budget="
				+ properties.getDailyRequestBudget()
				+ ", monthly Flow hard budget="
				+ properties.getMonthlyRequestBudget()
				+ " (no Pay As You Grow / silent paid switch). Traffic Incidents "
				+ (properties.isIncidentsEnabled() ? "enabled" : "disabled")
				+ " (cap "
				+ properties.getIncidentsMonthlyRequestBudget()
				+ "/month if ever enabled). TomTom payloads are not persisted.";

		return new ProviderStatusResponse(
				provider,
				configured ? health.getStatus() : ProviderHealthStatus.DISABLED,
				configured,
				properties.isScheduleEnabled(),
				properties.isIncidentsEnabled(),
				properties.getCacheTtlMinutes(),
				properties.getDailyRequestBudget(),
				properties.getMonthlyRequestBudget(),
				stats.getRequestCount(),
				requestsMonth,
				budgetService.remainingDaily(provider),
				budgetService.remainingMonthly(provider),
				stats.getSuccessCount(),
				stats.getFailureCount(),
				stats.getBudgetSkipped(),
				health.getLastSuccessAt(),
				health.getLastFailureAt(),
				health.getLastErrorCode(),
				health.getConsecutiveFailures(),
				List.copyOf(properties.getPilotSlugs()),
				List.copyOf(properties.getFlowZoomFallbacks()),
				usageNote);
	}

	public List<String> pilotSlugs() {
		return List.copyOf(properties.getPilotSlugs());
	}

	public boolean isPilot(String slug) {
		return properties.getPilotSlugs().stream().anyMatch(s -> s.equalsIgnoreCase(slug));
	}

	private ExternalTrafficResponse fetchUnderLock(Destination destination) {
		Object lock = flightLocks.computeIfAbsent(destination.getId(), id -> new Object());
		synchronized (lock) {
			Instant now = clock.instant();
			return cache.getFresh(destination.getId(), now)
					.map(observation -> ExternalTrafficResponse.from(observation, true))
					.orElseGet(() -> ExternalTrafficResponse.from(fetchAndCache(destination, now), true));
		}
	}

	private ExternalTrafficObservation fetchAndCache(Destination destination, Instant now) {
		String provider = trafficProvider.providerId();
		if (healthTracker.isCircuitOpen()) {
			return cache.getAny(destination.getId()).orElseGet(() -> unavailable(
					destination.getId(),
					"External traffic provider circuit is open after repeated failures."));
		}
		if (healthTracker.isRateLimited()) {
			return cache.getAny(destination.getId()).orElseGet(() -> unavailable(
					destination.getId(),
					"External traffic provider is backing off after a rate limit (429)."));
		}

		ProviderUnavailableException lastError = null;
		boolean attempted = false;
		List<Integer> zooms = properties.getFlowZoomFallbacks();
		if (zooms == null || zooms.isEmpty()) {
			zooms = List.of(12);
		}

		for (int zoom : zooms) {
			if (healthTracker.isRateLimited()) {
				break;
			}
			if (!budgetService.tryConsume(provider)) {
				if (!attempted) {
					return cache.getAny(destination.getId()).orElseGet(() -> unavailable(
							destination.getId(),
							"External traffic request budget reached (daily or monthly free-tier cap). Paid overages are disabled."));
				}
				break;
			}
			attempted = true;

			try {
				FlowSegmentReading reading = trafficProvider.fetchFlow(
						destination.getLatitude(), destination.getLongitude(), zoom);
				if (!FlowSegmentGeometryValidator.isNearDestination(
						destination.getLatitude(),
						destination.getLongitude(),
						reading,
						properties.getMaxSegmentDistanceMeters())) {
					lastError = new ProviderUnavailableException(
							"DISTANT_SEGMENT",
							"TomTom returned a road segment too far from the destination (zoom " + zoom + ")");
					budgetService.recordFailure(provider);
					continue;
				}

				ExternalTrafficStatus status = FlowSegmentNormalizer.toStatus(reading);
				String interpretation = interpretationFor(status, reading);
				ExternalTrafficObservation observation = new ExternalTrafficObservation(
						destination.getId(),
						provider,
						status,
						reading.currentSpeedKph(),
						reading.freeFlowSpeedKph(),
						reading.providerConfidence(),
						reading.functionalRoadClass(),
						now,
						now.plus(Duration.ofMinutes(properties.getCacheTtlMinutes())),
						properties.getAttribution(),
						null,
						true,
						ExternalTrafficObservation.SINGLE_ROAD_COVERAGE_NOTE,
						interpretation);
				cache.put(observation);
				budgetService.recordSuccess(provider);
				healthTracker.recordSuccess(provider);
				return observation;
			} catch (ProviderUnavailableException ex) {
				lastError = ex;
				budgetService.recordFailure(provider);
				healthTracker.recordFailure(provider, ex.getErrorCode());
				if ("RATE_LIMITED".equals(ex.getErrorCode()) || "NOT_CONFIGURED".equals(ex.getErrorCode())) {
					break;
				}
			}
		}

		if (!attempted) {
			return cache.getAny(destination.getId()).orElseGet(() -> unavailable(
					destination.getId(),
					"External traffic request budget reached (daily or monthly free-tier cap). Paid overages are disabled."));
		}

		ExternalTrafficObservation failed;
		if (lastError != null && "DISTANT_SEGMENT".equals(lastError.getErrorCode())) {
			failed = ExternalTrafficObservation.unknown(
					destination.getId(),
					provider,
					now,
					now.plus(Duration.ofMinutes(Math.min(5, properties.getCacheTtlMinutes()))),
					properties.getAttribution(),
					"No suitable nearby approach road segment found after zoom fallback.");
		} else {
			String message = lastError != null ? lastError.getMessage() : "External traffic provider unavailable";
			failed = unavailable(destination.getId(), message);
		}
		cache.put(failed);
		return failed;
	}

	private ExternalTrafficObservation unavailable(Long destinationId, String message) {
		Instant now = clock.instant();
		return ExternalTrafficObservation.unavailable(
				destinationId,
				trafficProvider.providerId(),
				now,
				now.plus(Duration.ofMinutes(Math.min(5, properties.getCacheTtlMinutes()))),
				properties.getAttribution(),
				message);
	}

	private static String interpretationFor(ExternalTrafficStatus status, FlowSegmentReading reading) {
		String speeds = "";
		if (reading.currentSpeedKph() != null && reading.freeFlowSpeedKph() != null) {
			speeds = " (" + reading.currentSpeedKph() + " vs " + reading.freeFlowSpeedKph() + " km/h free-flow)";
		}
		return switch (status) {
			case LIGHT -> "PDCM interpretation: light congestion on the sampled approach road" + speeds + ".";
			case MODERATE -> "PDCM interpretation: moderate congestion on the sampled approach road" + speeds + ".";
			case HEAVY -> "PDCM interpretation: heavy congestion on the sampled approach road" + speeds + ".";
			case UNKNOWN -> "PDCM interpretation: unknown — speeds incomplete for the sampled road.";
			case UNAVAILABLE -> "PDCM interpretation: external road data unavailable.";
		};
	}
}
