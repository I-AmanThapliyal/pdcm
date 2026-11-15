package com.pdcm.externaltraffic;

import com.pdcm.common.GeoUtils;

/**
 * Ensures the returned road fragment is geographically close enough to the
 * destination. Distant/unsuitable segments are rejected so PDCM does not
 * mislabel far-away roads as approach traffic.
 */
final class FlowSegmentGeometryValidator {

	private FlowSegmentGeometryValidator() {
	}

	static boolean isNearDestination(
			double destinationLat,
			double destinationLon,
			FlowSegmentReading reading,
			int maxDistanceMeters) {
		if (reading == null || reading.coordinates().isEmpty()) {
			return false;
		}
		double nearest = Double.POSITIVE_INFINITY;
		for (FlowSegmentCoordinate point : reading.coordinates()) {
			double meters = GeoUtils.haversineMeters(
					destinationLat, destinationLon, point.latitude(), point.longitude());
			if (meters < nearest) {
				nearest = meters;
			}
		}
		return nearest <= maxDistanceMeters;
	}
}
