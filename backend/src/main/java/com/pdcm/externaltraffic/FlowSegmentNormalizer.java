package com.pdcm.externaltraffic;

final class FlowSegmentNormalizer {

	private FlowSegmentNormalizer() {
	}

	static ExternalTrafficStatus toStatus(FlowSegmentReading reading) {
		if (reading == null) {
			return ExternalTrafficStatus.UNKNOWN;
		}
		if (reading.roadClosure()) {
			return ExternalTrafficStatus.HEAVY;
		}
		Integer current = reading.currentSpeedKph();
		Integer freeFlow = reading.freeFlowSpeedKph();
		if (current == null || freeFlow == null || freeFlow <= 0) {
			return ExternalTrafficStatus.UNKNOWN;
		}
		double ratio = current.doubleValue() / freeFlow.doubleValue();
		if (ratio >= 0.70) {
			return ExternalTrafficStatus.LIGHT;
		}
		if (ratio >= 0.40) {
			return ExternalTrafficStatus.MODERATE;
		}
		return ExternalTrafficStatus.HEAVY;
	}
}
