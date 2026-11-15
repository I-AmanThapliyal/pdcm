package com.pdcm.suggestion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SuggestionNameNormalizerTest {

	@Test
	void normalizesCaseWhitespaceAndPunctuation() {
		assertThat(SuggestionNameNormalizer.normalize("  Taj-Mahal!!!  "))
				.isEqualTo("taj mahal");
		assertThat(SuggestionNameNormalizer.normalize("Gateway   of India"))
				.isEqualTo("gateway of india");
	}

	@Test
	void slugifiesForCatalogue() {
		assertThat(SuggestionNameNormalizer.slugify("  Lotus Temple "))
				.isEqualTo("lotus-temple");
		assertThat(SuggestionNameNormalizer.slugify("!!!"))
				.isEqualTo("destination");
	}
}
