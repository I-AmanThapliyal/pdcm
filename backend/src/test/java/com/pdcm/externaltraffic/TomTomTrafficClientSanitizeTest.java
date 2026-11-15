package com.pdcm.externaltraffic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TomTomTrafficClientSanitizeTest {

	@Test
	void sanitizeForLog_redactsKeyQueryParamAndAbsoluteUrl() {
		ExternalTrafficProperties properties = new ExternalTrafficProperties();
		properties.setApiKey("super-secret-tomtom-key");
		TomTomTrafficClient client = new TomTomTrafficClient(properties);

		Exception ex = new RuntimeException(
				"GET https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/12/json?key=super-secret-tomtom-key&point=1,2 failed");

		String sanitized = client.sanitizeForLog(ex);

		assertThat(sanitized).doesNotContain("super-secret-tomtom-key");
		assertThat(sanitized).doesNotContain("https://api.tomtom.com");
		assertThat(sanitized).doesNotContain("api.tomtom.com");
		assertThat(sanitized).contains("[redacted-url]");
		assertThat(sanitized).startsWith("RuntimeException:");
	}

	@Test
	void sanitizeForLog_redactsStandaloneKeyQueryFragment() {
		ExternalTrafficProperties properties = new ExternalTrafficProperties();
		properties.setApiKey("another-secret");
		TomTomTrafficClient client = new TomTomTrafficClient(properties);

		String sanitized = client.sanitizeForLog(
				new RuntimeException("upstream said key=another-secret was invalid"));

		assertThat(sanitized).doesNotContain("another-secret");
		assertThat(sanitized).contains("key=***");
	}

	@Test
	void sanitizeForLog_redactsBareApiKeyIfPresent() {
		ExternalTrafficProperties properties = new ExternalTrafficProperties();
		properties.setApiKey("bare-key-value");
		TomTomTrafficClient client = new TomTomTrafficClient(properties);

		String sanitized = client.sanitizeForLog(new IllegalStateException("token bare-key-value leaked"));

		assertThat(sanitized).doesNotContain("bare-key-value");
		assertThat(sanitized).contains("***");
	}
}
