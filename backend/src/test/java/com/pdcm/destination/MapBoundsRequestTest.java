package com.pdcm.destination;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MapBoundsRequestTest {

	@Test
	void acceptsIndiaWideBoundsWithinSpan() {
		assertThatCode(() -> new MapBoundsRequest(6.5, 68.0, 35.5, 97.0).validate())
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsInvertedLatitude() {
		assertThatThrownBy(() -> new MapBoundsRequest(28.0, 77.0, 27.0, 78.0).validate())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("south must be less than north");
	}

	@Test
	void rejectsOversizeSpan() {
		assertThatThrownBy(() -> new MapBoundsRequest(0.0, 60.0, 40.0, 110.0).validate())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("40");
	}

	@Test
	void rejectsOutOfRangeCoordinates() {
		assertThatThrownBy(() -> new MapBoundsRequest(-100.0, 77.0, 27.0, 78.0).validate())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("valid latitude/longitude");
	}
}
