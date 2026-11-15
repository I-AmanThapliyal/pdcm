package com.pdcm.suggestion;

import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationCategory;

public record PossibleDuplicateResponse(
		Long destinationId,
		String name,
		String slug,
		String region,
		String city,
		DestinationCategory category,
		Double latitude,
		Double longitude,
		String matchReason,
		Double distanceKm
) {
	public static PossibleDuplicateResponse from(Destination destination, String matchReason, Double distanceKm) {
		return new PossibleDuplicateResponse(
				destination.getId(),
				destination.getName(),
				destination.getSlug(),
				destination.getRegion(),
				destination.getCity(),
				destination.getCategory(),
				destination.getLatitude(),
				destination.getLongitude(),
				matchReason,
				distanceKm);
	}
}
