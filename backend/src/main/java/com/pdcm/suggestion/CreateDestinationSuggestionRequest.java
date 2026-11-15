package com.pdcm.suggestion;

import com.pdcm.destination.DestinationCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDestinationSuggestionRequest(
		@NotBlank(message = "name is required")
		@Size(max = 200)
		String name,

		@NotBlank(message = "city is required")
		@Size(max = 120)
		String city,

		@NotBlank(message = "region (state/UT) is required")
		@Size(max = 100)
		String region,

		@NotNull(message = "category is required")
		DestinationCategory category,

		@NotBlank(message = "description is required")
		@Size(max = 4000)
		String description,

		Double latitude,

		Double longitude,

		@Size(max = 500)
		String referenceUrl
) {
}
