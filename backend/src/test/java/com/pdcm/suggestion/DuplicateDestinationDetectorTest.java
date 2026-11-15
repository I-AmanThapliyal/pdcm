package com.pdcm.suggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationCategory;
import com.pdcm.destination.DestinationRepository;

@ExtendWith(MockitoExtension.class)
class DuplicateDestinationDetectorTest {

	@Mock
	DestinationRepository destinationRepository;

	@InjectMocks
	DuplicateDestinationDetector detector;

	@Test
	void matchesSameNormalizedNameAndRegion() {
		Destination existing = new Destination(
				"Taj Mahal",
				"taj-mahal",
				"Iconic marble mausoleum.",
				"Uttar Pradesh",
				"Agra",
				DestinationCategory.HISTORICAL,
				27.1751,
				78.0421,
				"LEGACY_SEED",
				null,
				"UNKNOWN");
		when(destinationRepository.findAllActive()).thenReturn(List.of(existing));

		List<PossibleDuplicateResponse> matches = detector.findPossibleDuplicates(
				"  taj-mahal ",
				"Uttar Pradesh",
				27.1752,
				78.0422);

		assertThat(matches).hasSize(1);
		assertThat(matches.get(0).matchReason()).isEqualTo("SAME_NAME_AND_REGION");
		assertThat(matches.get(0).distanceKm()).isNotNull();
		assertThat(matches.get(0).distanceKm()).isLessThan(1.0);
	}

	@Test
	void matchesProximityWithSimilarName() {
		Destination existing = new Destination(
				"Gateway of India",
				"gateway-of-india",
				"Mumbai waterfront arch.",
				"Maharashtra",
				"Mumbai",
				DestinationCategory.HISTORICAL,
				18.9220,
				72.8347,
				"LEGACY_SEED",
				null,
				"UNKNOWN");
		when(destinationRepository.findAllActive()).thenReturn(List.of(existing));

		List<PossibleDuplicateResponse> matches = detector.findPossibleDuplicates(
				"Gateway of India Monument",
				"Maharashtra",
				18.9221,
				72.8348);

		assertThat(matches).isNotEmpty();
		assertThat(matches.get(0).matchReason()).isIn(
				"SIMILAR_NAME_AND_REGION",
				"PROXIMITY_AND_CONTEXT",
				"SAME_NAME_AND_PROXIMITY");
	}

	@Test
	void ignoresDistantDifferentName() {
		Destination existing = new Destination(
				"India Gate",
				"india-gate",
				"Delhi war memorial.",
				"Delhi",
				"New Delhi",
				DestinationCategory.HISTORICAL,
				28.6129,
				77.2295,
				"LEGACY_SEED",
				null,
				"UNKNOWN");
		when(destinationRepository.findAllActive()).thenReturn(List.of(existing));

		List<PossibleDuplicateResponse> matches = detector.findPossibleDuplicates(
				"Completely Different Place",
				"Kerala",
				8.4,
				76.9);

		assertThat(matches).isEmpty();
	}
}
