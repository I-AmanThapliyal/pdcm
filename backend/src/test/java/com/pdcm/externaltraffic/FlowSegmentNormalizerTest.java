package com.pdcm.externaltraffic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FlowSegmentNormalizerTest {

	@Test
	void mapsRatiosToBands() {
		assertThat(FlowSegmentNormalizer.toStatus(new FlowSegmentReading(70, 100, 0.9, false, "FRC2")))
				.isEqualTo(ExternalTrafficStatus.LIGHT);
		assertThat(FlowSegmentNormalizer.toStatus(new FlowSegmentReading(50, 100, 0.9, false, "FRC2")))
				.isEqualTo(ExternalTrafficStatus.MODERATE);
		assertThat(FlowSegmentNormalizer.toStatus(new FlowSegmentReading(20, 100, 0.9, false, "FRC2")))
				.isEqualTo(ExternalTrafficStatus.HEAVY);
	}

	@Test
	void roadClosureIsHeavy() {
		assertThat(FlowSegmentNormalizer.toStatus(new FlowSegmentReading(60, 60, 1.0, true, "FRC1")))
				.isEqualTo(ExternalTrafficStatus.HEAVY);
	}

	@Test
	void missingSpeedsAreUnknown() {
		assertThat(FlowSegmentNormalizer.toStatus(new FlowSegmentReading(null, 40, null, false, null)))
				.isEqualTo(ExternalTrafficStatus.UNKNOWN);
	}
}
