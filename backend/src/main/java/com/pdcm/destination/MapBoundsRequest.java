package com.pdcm.destination;

public record MapBoundsRequest(
		double south,
		double west,
		double north,
		double east
) {
	public static final int DEFAULT_LIMIT = 100;
	public static final int MAX_LIMIT = 200;
	public static final double MAX_SPAN_DEGREES = 40.0;

	public void validate() {
		if (south < -90 || south > 90 || north < -90 || north > 90
				|| west < -180 || west > 180 || east < -180 || east > 180) {
			throw new IllegalArgumentException("Map bounds must use valid latitude/longitude values");
		}
		if (south >= north) {
			throw new IllegalArgumentException("south must be less than north");
		}
		if (west >= east) {
			throw new IllegalArgumentException("west must be less than east (antimeridian spans are not supported)");
		}
		if ((north - south) > MAX_SPAN_DEGREES || (east - west) > MAX_SPAN_DEGREES) {
			throw new IllegalArgumentException("Map bounds span must be at most " + MAX_SPAN_DEGREES + " degrees");
		}
	}
}
