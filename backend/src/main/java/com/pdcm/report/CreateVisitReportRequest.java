package com.pdcm.report;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVisitReportRequest(
		@NotNull(message = "crowdIntensity is required")
		@Min(value = 1, message = "crowdIntensity must be between 1 and 5")
		@Max(value = 5, message = "crowdIntensity must be between 1 and 5")
		Integer crowdIntensity,
		@NotNull TrafficCondition trafficCondition,
		@Min(0) @Max(600) Integer waitMinutes,
		@Size(max = 500) String note,
		@NotNull Instant observedAt,
		Boolean locationConsent,
		@Valid ClientLocationPayload clientLocation
) {
}
