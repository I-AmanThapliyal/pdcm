package com.pdcm.externaltraffic;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderHealthTracker {

	private final ProviderHealthRepository healthRepository;
	private final ExternalTrafficProperties properties;
	private final Clock clock;
	private final AtomicReference<Instant> circuitOpenedAt = new AtomicReference<>();
	private final AtomicReference<Instant> rateLimitedUntil = new AtomicReference<>();
	private final ConcurrentHashMap<String, Object> ensureLocks = new ConcurrentHashMap<>();

	public ProviderHealthTracker(
			ProviderHealthRepository healthRepository,
			ExternalTrafficProperties properties,
			Clock clock) {
		this.healthRepository = healthRepository;
		this.properties = properties;
		this.clock = clock;
	}

	public boolean isCircuitOpen() {
		Instant opened = circuitOpenedAt.get();
		if (opened == null) {
			return false;
		}
		Instant now = clock.instant();
		if (now.isAfter(opened.plusSeconds(properties.getCircuitOpenMinutes() * 60L))) {
			circuitOpenedAt.compareAndSet(opened, null);
			return false;
		}
		return true;
	}

	public boolean isRateLimited() {
		Instant until = rateLimitedUntil.get();
		if (until == null) {
			return false;
		}
		Instant now = clock.instant();
		if (now.isAfter(until)) {
			rateLimitedUntil.compareAndSet(until, null);
			return false;
		}
		return true;
	}

	@Transactional
	public ProviderHealth ensureRow(String provider) {
		return healthRepository.findById(provider).orElseGet(() -> {
			Object lock = ensureLocks.computeIfAbsent(provider, key -> new Object());
			synchronized (lock) {
				return healthRepository.findById(provider).orElseGet(() ->
						healthRepository.save(new ProviderHealth(provider, ProviderHealthStatus.DISABLED)));
			}
		});
	}

	@Transactional
	public void markConfigured(String provider, boolean configured) {
		ProviderHealth health = ensureRow(provider);
		if (!configured) {
			health.setStatus(ProviderHealthStatus.DISABLED);
		} else if (health.getStatus() == ProviderHealthStatus.DISABLED) {
			health.setStatus(ProviderHealthStatus.UP);
		}
		health.setUpdatedAt(clock.instant());
	}

	@Transactional
	public void recordSuccess(String provider) {
		ProviderHealth health = ensureRow(provider);
		Instant now = clock.instant();
		health.setLastSuccessAt(now);
		health.setConsecutiveFailures(0);
		health.setLastErrorCode(null);
		health.setStatus(ProviderHealthStatus.UP);
		health.setUpdatedAt(now);
		circuitOpenedAt.set(null);
		rateLimitedUntil.set(null);
	}

	@Transactional
	public void recordFailure(String provider, String errorCode) {
		ProviderHealth health = ensureRow(provider);
		Instant now = clock.instant();
		health.setLastFailureAt(now);
		health.setLastErrorCode(errorCode);
		int failures = health.getConsecutiveFailures() + 1;
		health.setConsecutiveFailures(failures);
		if ("RATE_LIMITED".equals(errorCode)) {
			rateLimitedUntil.set(now.plusSeconds(Math.max(1, properties.getRateLimitBackoffMinutes()) * 60L));
		}
		if (failures >= properties.getMaxConsecutiveFailuresBeforeOpen()) {
			health.setStatus(ProviderHealthStatus.DOWN);
			circuitOpenedAt.set(now);
		} else {
			health.setStatus(ProviderHealthStatus.DEGRADED);
		}
		health.setUpdatedAt(now);
	}

	@Transactional(readOnly = true)
	public ProviderHealth read(String provider) {
		return healthRepository.findById(provider)
				.orElseGet(() -> new ProviderHealth(provider, ProviderHealthStatus.DISABLED));
	}
}
