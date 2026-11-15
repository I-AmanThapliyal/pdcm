package com.pdcm.externaltraffic;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ExternalTrafficCache {

	private final ConcurrentHashMap<Long, ExternalTrafficObservation> byDestination = new ConcurrentHashMap<>();

	public Optional<ExternalTrafficObservation> getFresh(Long destinationId, Instant now) {
		ExternalTrafficObservation observation = byDestination.get(destinationId);
		if (observation == null) {
			return Optional.empty();
		}
		if (observation.expiresAt().isBefore(now)) {
			return Optional.empty();
		}
		return Optional.of(observation);
	}

	public Optional<ExternalTrafficObservation> getAny(Long destinationId) {
		return Optional.ofNullable(byDestination.get(destinationId));
	}

	public void put(ExternalTrafficObservation observation) {
		byDestination.put(observation.destinationId(), observation);
	}

	public void clear() {
		byDestination.clear();
	}
}
