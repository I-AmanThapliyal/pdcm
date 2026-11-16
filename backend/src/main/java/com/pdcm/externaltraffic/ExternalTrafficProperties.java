package com.pdcm.externaltraffic;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pdcm.external-traffic")
public class ExternalTrafficProperties {

	private boolean enabled = true;
	private String provider = "TOMTOM";
	/** Set via PDCM_TOMTOM_API_KEY. Never expose to the frontend. */
	private String apiKey = "";
	private String baseUrl = "https://api.tomtom.com";
	private String flowPathTemplate = "/traffic/services/4/flowSegmentData/absolute/{zoom}/json";
	/**
	 * Zoom fallback order for Flow Segment. Stop at the first geographically valid
	 * response. Each attempt that hits TomTom counts against the request budget.
	 */
	private List<Integer> flowZoomFallbacks = new ArrayList<>(List.of(18, 15, 12, 10));
	/** Shared destination cache TTL (desired refresh: 3 hours). */
	private int cacheTtlMinutes = 180;
	/** Must stay false — on-demand only; no background polling. */
	private boolean scheduleEnabled = false;
	private long scheduleFixedDelayMs = 10_800_000L;
	/**
	 * Hard daily cap (UTC). Default ~15_000/month ÷ 30. No Pay As You Grow.
	 */
	private int dailyRequestBudget = 500;
	/**
	 * Hard monthly Flow Segment cap (UTC month). Free-tier internal limit.
	 */
	private int monthlyRequestBudget = 15_000;
	/**
	 * Traffic Incidents stay disabled in this pass (would be 1_500/month if enabled).
	 */
	private boolean incidentsEnabled = false;
	private int incidentsMonthlyRequestBudget = 1_500;
	/** Reject segments whose nearest geometry point is farther than this. */
	private int maxSegmentDistanceMeters = 750;
	private int connectTimeoutMs = 3_000;
	private int readTimeoutMs = 5_000;
	private int maxConsecutiveFailuresBeforeOpen = 5;
	private int circuitOpenMinutes = 15;
	/** Backoff after HTTP 429 before allowing further TomTom calls. */
	private int rateLimitBackoffMinutes = 15;
	private String attribution = "Traffic data © TomTom";
	/**
	 * Slug-based TomTom Flow allowlist. Keep in sync with
	 * {@code pdcm.external-traffic.pilot-slugs}. Only USABLE geometry from
	 * docs/TOMTOM_PILOT_COVERAGE.md (52 destinations). taj-mahal failed validation.
	 */
	private List<String> pilotSlugs = new ArrayList<>(List.of(
			"india-gate",
			"red-fort",
			"qutb-minar",
			"lotus-temple",
			"fatehpur-sikri",
			"amber-fort",
			"hawa-mahal",
			"jaipur-city-palace",
			"jantar-mantar-jaipur",
			"nahargarh-fort",
			"gateway-of-india",
			"marine-drive",
			"chhatrapati-shivaji-maharaj-terminus",
			"haridwar",
			"mussoorie",
			"nainital",
			"shimla-ridge",
			"golden-temple",
			"manali-mall-road",
			"darjeeling-mall",
			"charminar",
			"mysore-palace",
			"hampi-virupaksha-temple",
			"pondicherry-promenade",
			"meenakshi-temple",
			"munnar",
			"ooty",
			"kodaikanal",
			"goa-calangute-beach",
			"cherai-beach",
			"kovalam-beach",
			"brihadeeswarar-temple",
			"city-palace-udaipur",
			"mehrangarh-fort",
			"jodhpur-blue-city-views",
			"jaisalmer-fort",
			"pushkar",
			"sabarmati-ashram",
			"statue-of-unity",
			"howrah-bridge",
			"victoria-memorial",
			"belur-math",
			"shaniwar-wada",
			"aga-khan-palace",
			"lonavala",
			"khajuraho-temples",
			"konark-sun-temple",
			"ujjain",
			"tirupati",
			"kanyakumari",
			"gokarna",
			"junagarh-fort"));


	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getFlowPathTemplate() {
		return flowPathTemplate;
	}

	public void setFlowPathTemplate(String flowPathTemplate) {
		this.flowPathTemplate = flowPathTemplate;
	}

	public List<Integer> getFlowZoomFallbacks() {
		return flowZoomFallbacks;
	}

	public void setFlowZoomFallbacks(List<Integer> flowZoomFallbacks) {
		this.flowZoomFallbacks = flowZoomFallbacks;
	}

	public int getCacheTtlMinutes() {
		return cacheTtlMinutes;
	}

	public void setCacheTtlMinutes(int cacheTtlMinutes) {
		this.cacheTtlMinutes = cacheTtlMinutes;
	}

	public boolean isScheduleEnabled() {
		return scheduleEnabled;
	}

	public void setScheduleEnabled(boolean scheduleEnabled) {
		this.scheduleEnabled = scheduleEnabled;
	}

	public long getScheduleFixedDelayMs() {
		return scheduleFixedDelayMs;
	}

	public void setScheduleFixedDelayMs(long scheduleFixedDelayMs) {
		this.scheduleFixedDelayMs = scheduleFixedDelayMs;
	}

	public int getDailyRequestBudget() {
		return dailyRequestBudget;
	}

	public void setDailyRequestBudget(int dailyRequestBudget) {
		this.dailyRequestBudget = dailyRequestBudget;
	}

	public int getMonthlyRequestBudget() {
		return monthlyRequestBudget;
	}

	public void setMonthlyRequestBudget(int monthlyRequestBudget) {
		this.monthlyRequestBudget = monthlyRequestBudget;
	}

	public boolean isIncidentsEnabled() {
		return incidentsEnabled;
	}

	public void setIncidentsEnabled(boolean incidentsEnabled) {
		this.incidentsEnabled = incidentsEnabled;
	}

	public int getIncidentsMonthlyRequestBudget() {
		return incidentsMonthlyRequestBudget;
	}

	public void setIncidentsMonthlyRequestBudget(int incidentsMonthlyRequestBudget) {
		this.incidentsMonthlyRequestBudget = incidentsMonthlyRequestBudget;
	}

	public int getMaxSegmentDistanceMeters() {
		return maxSegmentDistanceMeters;
	}

	public void setMaxSegmentDistanceMeters(int maxSegmentDistanceMeters) {
		this.maxSegmentDistanceMeters = maxSegmentDistanceMeters;
	}

	public int getConnectTimeoutMs() {
		return connectTimeoutMs;
	}

	public void setConnectTimeoutMs(int connectTimeoutMs) {
		this.connectTimeoutMs = connectTimeoutMs;
	}

	public int getReadTimeoutMs() {
		return readTimeoutMs;
	}

	public void setReadTimeoutMs(int readTimeoutMs) {
		this.readTimeoutMs = readTimeoutMs;
	}

	public int getMaxConsecutiveFailuresBeforeOpen() {
		return maxConsecutiveFailuresBeforeOpen;
	}

	public void setMaxConsecutiveFailuresBeforeOpen(int maxConsecutiveFailuresBeforeOpen) {
		this.maxConsecutiveFailuresBeforeOpen = maxConsecutiveFailuresBeforeOpen;
	}

	public int getCircuitOpenMinutes() {
		return circuitOpenMinutes;
	}

	public void setCircuitOpenMinutes(int circuitOpenMinutes) {
		this.circuitOpenMinutes = circuitOpenMinutes;
	}

	public int getRateLimitBackoffMinutes() {
		return rateLimitBackoffMinutes;
	}

	public void setRateLimitBackoffMinutes(int rateLimitBackoffMinutes) {
		this.rateLimitBackoffMinutes = rateLimitBackoffMinutes;
	}

	public String getAttribution() {
		return attribution;
	}

	public void setAttribution(String attribution) {
		this.attribution = attribution;
	}

	public List<String> getPilotSlugs() {
		return pilotSlugs;
	}

	public void setPilotSlugs(List<String> pilotSlugs) {
		this.pilotSlugs = pilotSlugs;
	}

	public boolean hasApiKey() {
		return apiKey != null && !apiKey.isBlank();
	}
}
