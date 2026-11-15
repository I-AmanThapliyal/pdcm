package com.pdcm.report;

import java.time.Instant;

/**
 * One user's latest in-window observation used by crowd and traffic aggregation.
 */
public record CrowdObservation(
		long userId,
		int crowdIntensity,
		TrafficCondition traffic,
		Instant observedAt
) {
}
