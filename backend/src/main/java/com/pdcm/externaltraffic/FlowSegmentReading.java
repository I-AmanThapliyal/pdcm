package com.pdcm.externaltraffic;

import java.util.List;

public record FlowSegmentReading(
		Integer currentSpeedKph,
		Integer freeFlowSpeedKph,
		Double providerConfidence,
		boolean roadClosure,
		String functionalRoadClass,
		List<FlowSegmentCoordinate> coordinates
) {
	public FlowSegmentReading {
		coordinates = coordinates == null ? List.of() : List.copyOf(coordinates);
	}

	/** Test/helper constructor without geometry. */
	public FlowSegmentReading(
			Integer currentSpeedKph,
			Integer freeFlowSpeedKph,
			Double providerConfidence,
			boolean roadClosure,
			String functionalRoadClass) {
		this(currentSpeedKph, freeFlowSpeedKph, providerConfidence, roadClosure, functionalRoadClass, List.of());
	}
}
