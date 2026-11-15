package com.pdcm.suggestion;

import jakarta.validation.constraints.NotNull;

/**
 * ADMIN-only body for setting or correcting suggestion coordinates.
 * Both values are required; WGS84 and soft India checks run in the service.
 */
public record UpdateSuggestionCoordinatesRequest(
		@NotNull(message = "latitude is required")
		Double latitude,

		@NotNull(message = "longitude is required")
		Double longitude
) {
}
