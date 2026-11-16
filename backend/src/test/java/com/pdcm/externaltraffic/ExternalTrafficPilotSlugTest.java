package com.pdcm.externaltraffic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ExternalTrafficPilotSlugTest {

	private static final List<String> RETAINED_ORIGINALS = List.of(
			"amber-fort",
			"gateway-of-india",
			"hawa-mahal",
			"india-gate");

	@Test
	void defaultPilotSlugs_areUnique_matchVerifiedUsableCount() {
		ExternalTrafficProperties properties = new ExternalTrafficProperties();
		List<String> slugs = properties.getPilotSlugs();

		assertThat(slugs).hasSize(52);
		assertThat(slugs).containsAll(RETAINED_ORIGINALS);
		assertThat(slugs).doesNotContain("taj-mahal");

		Set<String> unique = new HashSet<>();
		for (String slug : slugs) {
			assertThat(slug).isNotBlank();
			assertThat(unique.add(slug.toLowerCase()))
					.as("duplicate pilot slug: %s", slug)
					.isTrue();
		}
		assertThat(unique).hasSize(52);
	}
}
