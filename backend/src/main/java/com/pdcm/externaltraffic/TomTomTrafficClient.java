package com.pdcm.externaltraffic;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class TomTomTrafficClient implements TrafficProvider {

	private static final Logger log = LoggerFactory.getLogger(TomTomTrafficClient.class);

	/** Redact API key query params and absolute URLs that may embed them. */
	private static final Pattern KEY_PARAM = Pattern.compile("(?i)([?&]key=)[^&\\s\"']+");
	private static final Pattern ABSOLUTE_URL = Pattern.compile("https?://\\S+");

	private final ExternalTrafficProperties properties;
	private final JsonMapper jsonMapper;
	private final HttpClient httpClient;

	public TomTomTrafficClient(ExternalTrafficProperties properties) {
		this(properties, JsonMapper.builder().build(), HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(Math.max(1, properties.getConnectTimeoutMs())))
				.build());
	}

	TomTomTrafficClient(ExternalTrafficProperties properties, JsonMapper jsonMapper, HttpClient httpClient) {
		this.properties = properties;
		this.jsonMapper = jsonMapper;
		this.httpClient = httpClient;
	}

	@Override
	public String providerId() {
		return "TOMTOM";
	}

	@Override
	public FlowSegmentReading fetchFlow(double latitude, double longitude, int zoom) {
		if (!properties.hasApiKey()) {
			throw new ProviderUnavailableException("NOT_CONFIGURED", "TomTom API key is not configured");
		}

		URI uri = UriComponentsBuilder
				.fromUriString(trimTrailingSlash(properties.getBaseUrl()) + properties.getFlowPathTemplate()
						.replace("{zoom}", String.valueOf(zoom)))
				.queryParam("key", properties.getApiKey())
				.queryParam("point", latitude + "," + longitude)
				.queryParam("unit", "KMPH")
				.build(true)
				.toUri();

		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofMillis(Math.max(1, properties.getReadTimeoutMs())))
				.GET()
				.header("Accept", "application/json")
				.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			int code = response.statusCode();
			if (code == 429) {
				throw new ProviderUnavailableException("RATE_LIMITED", "TomTom rate limit (429)");
			}
			if (code < 200 || code >= 300 || response.body() == null || response.body().isBlank()) {
				throw new ProviderUnavailableException("HTTP_" + code, "TomTom returned HTTP " + code);
			}
			return parse(response.body());
		}
		catch (ProviderUnavailableException ex) {
			throw ex;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ProviderUnavailableException("INTERRUPTED", "TomTom request interrupted");
		}
		catch (Exception ex) {
			log.warn("TomTom flow segment request failed (zoom={}): {}", zoom, sanitizeForLog(ex));
			// Public exception message must never include URI or key.
			throw new ProviderUnavailableException("CLIENT_ERROR", "TomTom request failed");
		}
	}

	/**
	 * Never log API keys or complete request URLs. HTTP client exceptions may embed them.
	 */
	String sanitizeForLog(Throwable ex) {
		if (ex == null) {
			return "unknown";
		}
		String type = ex.getClass().getSimpleName();
		String message = ex.getMessage();
		if (message == null || message.isBlank()) {
			return type;
		}
		String redacted = KEY_PARAM.matcher(message).replaceAll("$1***");
		redacted = ABSOLUTE_URL.matcher(redacted).replaceAll("[redacted-url]");
		if (properties.hasApiKey()) {
			redacted = redacted.replace(properties.getApiKey(), "***");
		}
		return type + ": " + redacted;
	}

	FlowSegmentReading parse(String body) {
		JsonNode root = jsonMapper.readTree(body);
		JsonNode data = root.path("flowSegmentData");
		if (data.isMissingNode() || data.isNull()) {
			throw new ProviderUnavailableException("BAD_PAYLOAD", "TomTom response missing flowSegmentData");
		}
		Integer current = intOrNull(data.get("currentSpeed"));
		Integer freeFlow = intOrNull(data.get("freeFlowSpeed"));
		Double confidence = data.hasNonNull("confidence") ? data.get("confidence").asDouble() : null;
		boolean closure = data.path("roadClosure").asBoolean(false);
		String frc = data.hasNonNull("frc") ? data.get("frc").asText() : null;
		List<FlowSegmentCoordinate> coordinates = parseCoordinates(data.path("coordinates"));
		return new FlowSegmentReading(current, freeFlow, confidence, closure, frc, coordinates);
	}

	private static List<FlowSegmentCoordinate> parseCoordinates(JsonNode coordinatesNode) {
		List<FlowSegmentCoordinate> points = new ArrayList<>();
		if (coordinatesNode == null || coordinatesNode.isMissingNode() || coordinatesNode.isNull()) {
			return points;
		}
		JsonNode list = coordinatesNode.path("coordinate");
		if (!list.isArray()) {
			return points;
		}
		for (JsonNode point : list) {
			if (point == null || !point.hasNonNull("latitude") || !point.hasNonNull("longitude")) {
				continue;
			}
			points.add(new FlowSegmentCoordinate(point.get("latitude").asDouble(), point.get("longitude").asDouble()));
		}
		return points;
	}

	private static Integer intOrNull(JsonNode node) {
		if (node == null || node.isNull() || !node.isNumber()) {
			return null;
		}
		return node.asInt();
	}

	private static String trimTrailingSlash(String value) {
		if (value.endsWith("/")) {
			return value.substring(0, value.length() - 1);
		}
		return value;
	}
}
