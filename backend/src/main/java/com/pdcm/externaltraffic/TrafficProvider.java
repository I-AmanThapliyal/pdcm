package com.pdcm.externaltraffic;

public interface TrafficProvider {

	String providerId();

	/**
	 * One Flow Segment request at the given zoom. Callers own zoom fallback and
	 * budget accounting (each call may count as one provider request).
	 */
	FlowSegmentReading fetchFlow(double latitude, double longitude, int zoom);
}
