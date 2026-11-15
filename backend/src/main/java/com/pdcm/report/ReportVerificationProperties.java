package com.pdcm.report;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pdcm.report-verification")
public class ReportVerificationProperties {

	/**
	 * Max Haversine distance from destination centre for LOCATION_VERIFIED (metres).
	 * Large enough for expansive sites; proximity signal only.
	 */
	private double geofenceRadiusMeters = 500.0;

	/**
	 * GPS accuracy worse than this cannot become LOCATION_VERIFIED.
	 */
	private double maxAccuracyMeters = 150.0;

	/**
	 * How long raw client lat/lon/accuracy may be retained after report creation.
	 * Default 24 hours. After purge, verification metadata (status, reason, distance) is kept.
	 */
	private Duration coordinateRetention = Duration.ofHours(24);

	/**
	 * When true, scheduled purge runs. Safe to leave enabled in all environments.
	 */
	private boolean purgeEnabled = true;

	/**
	 * Fixed delay between purge job runs.
	 */
	private long purgeFixedDelayMs = 300_000L;

	/**
	 * Rough WGS84 box used only as a soft spoofing gate (not a hard proof).
	 */
	private double indiaLatMin = 6.0;
	private double indiaLatMax = 37.5;
	private double indiaLonMin = 68.0;
	private double indiaLonMax = 98.0;

	public double getGeofenceRadiusMeters() {
		return geofenceRadiusMeters;
	}

	public void setGeofenceRadiusMeters(double geofenceRadiusMeters) {
		this.geofenceRadiusMeters = geofenceRadiusMeters;
	}

	public double getMaxAccuracyMeters() {
		return maxAccuracyMeters;
	}

	public void setMaxAccuracyMeters(double maxAccuracyMeters) {
		this.maxAccuracyMeters = maxAccuracyMeters;
	}

	public Duration getCoordinateRetention() {
		return coordinateRetention;
	}

	public void setCoordinateRetention(Duration coordinateRetention) {
		this.coordinateRetention = coordinateRetention;
	}

	public boolean isPurgeEnabled() {
		return purgeEnabled;
	}

	public void setPurgeEnabled(boolean purgeEnabled) {
		this.purgeEnabled = purgeEnabled;
	}

	public long getPurgeFixedDelayMs() {
		return purgeFixedDelayMs;
	}

	public void setPurgeFixedDelayMs(long purgeFixedDelayMs) {
		this.purgeFixedDelayMs = purgeFixedDelayMs;
	}

	public double getIndiaLatMin() {
		return indiaLatMin;
	}

	public void setIndiaLatMin(double indiaLatMin) {
		this.indiaLatMin = indiaLatMin;
	}

	public double getIndiaLatMax() {
		return indiaLatMax;
	}

	public void setIndiaLatMax(double indiaLatMax) {
		this.indiaLatMax = indiaLatMax;
	}

	public double getIndiaLonMin() {
		return indiaLonMin;
	}

	public void setIndiaLonMin(double indiaLonMin) {
		this.indiaLonMin = indiaLonMin;
	}

	public double getIndiaLonMax() {
		return indiaLonMax;
	}

	public void setIndiaLonMax(double indiaLonMax) {
		this.indiaLonMax = indiaLonMax;
	}
}
