package com.pdcm.destination;

public record NearbyDestinationResponse(
		DestinationResponse destination,
		double distanceKm
) {
}
