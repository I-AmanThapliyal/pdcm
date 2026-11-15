package com.pdcm.externaltraffic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.http.HttpClient;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class TomTomTrafficClientTest {

	@Test
	void parsesFlowSegmentPayloadWithCoordinates() {
		ExternalTrafficProperties properties = new ExternalTrafficProperties();
		properties.setApiKey("test-key");
		TomTomTrafficClient client = new TomTomTrafficClient(
				properties, JsonMapper.builder().build(), HttpClient.newHttpClient());

		FlowSegmentReading reading = client.parse("""
				{
				  "flowSegmentData": {
				    "frc": "FRC3",
				    "currentSpeed": 22,
				    "freeFlowSpeed": 55,
				    "confidence": 0.91,
				    "roadClosure": false,
				    "coordinates": {
				      "coordinate": [
				        {"latitude": 27.1751, "longitude": 78.0421},
				        {"latitude": 27.1752, "longitude": 78.0423}
				      ]
				    }
				  }
				}
				""");

		assertThat(reading.currentSpeedKph()).isEqualTo(22);
		assertThat(reading.freeFlowSpeedKph()).isEqualTo(55);
		assertThat(reading.providerConfidence()).isEqualTo(0.91);
		assertThat(reading.functionalRoadClass()).isEqualTo("FRC3");
		assertThat(reading.coordinates()).hasSize(2);
		assertThat(reading.coordinates().get(0).latitude()).isEqualTo(27.1751);
	}

	@Test
	void rejectsMissingFlowSegment() {
		ExternalTrafficProperties properties = new ExternalTrafficProperties();
		properties.setApiKey("test-key");
		TomTomTrafficClient client = new TomTomTrafficClient(
				properties, JsonMapper.builder().build(), HttpClient.newHttpClient());

		assertThatThrownBy(() -> client.parse("{\"hello\":true}"))
				.isInstanceOf(ProviderUnavailableException.class)
				.extracting(ex -> ((ProviderUnavailableException) ex).getErrorCode())
				.isEqualTo("BAD_PAYLOAD");
	}
}
