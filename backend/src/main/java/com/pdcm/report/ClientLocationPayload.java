package com.pdcm.report;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Optional GPS sample from the client. Stored privately; never returned on public APIs.
 */
public record ClientLocationPayload(
		@NotNull
		@DecimalMin(value = "-90.0")
		@DecimalMax(value = "90.0")
		Double latitude,
		@NotNull
		@DecimalMin(value = "-180.0")
		@DecimalMax(value = "180.0")
		Double longitude,
		@DecimalMin(value = "0.0")
		@DecimalMax(value = "100000.0")
		Double accuracyMeters
) {
}
