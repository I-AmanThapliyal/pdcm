package com.pdcm.externaltraffic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class FlowSegmentGeometryValidatorTest {

	@Test
	void acceptsNearbySegment() {
		FlowSegmentReading reading = new FlowSegmentReading(
				40,
				60,
				0.9,
				false,
				"FRC3",
				List.of(new FlowSegmentCoordinate(27.1751, 78.0421)));

		assertThat(FlowSegmentGeometryValidator.isNearDestination(27.1750, 78.0420, reading, 750))
				.isTrue();
	}

	@Test
	void rejectsDistantSegment() {
		FlowSegmentReading reading = new FlowSegmentReading(
				40,
				60,
				0.9,
				false,
				"FRC3",
				List.of(new FlowSegmentCoordinate(1.0, 1.0)));

		assertThat(FlowSegmentGeometryValidator.isNearDestination(27.1750, 78.0420, reading, 750))
				.isFalse();
	}

	@Test
	void rejectsMissingCoordinates() {
		FlowSegmentReading reading = new FlowSegmentReading(40, 60, 0.9, false, "FRC3");
		assertThat(FlowSegmentGeometryValidator.isNearDestination(27.1750, 78.0420, reading, 750))
				.isFalse();
	}
}
