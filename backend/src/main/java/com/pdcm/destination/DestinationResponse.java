package com.pdcm.destination;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

public record DestinationResponse(
		Long id,
		String name,
		String slug,
		String description,
		String region,
		String city,
		DestinationCategory category,
		Double latitude,
		Double longitude,
		String crowdSensitivity,
		String sourceReference,
		String sourceUrl,
		String sourceLicense,
		Set<String> tags,
		Instant createdAt,
		Instant updatedAt
) {
	public static DestinationResponse from(Destination d) {
		return new DestinationResponse(
				d.getId(),
				d.getName(),
				d.getSlug(),
				d.getDescription(),
				d.getRegion(),
				d.getCity(),
				d.getCategory(),
				d.getLatitude(),
				d.getLongitude(),
				d.getCrowdSensitivity(),
				d.getSourceReference(),
				d.getSourceUrl(),
				d.getSourceLicense(),
				new TreeSet<>(d.getTags()),
				d.getCreatedAt(),
				d.getUpdatedAt());
	}
}
