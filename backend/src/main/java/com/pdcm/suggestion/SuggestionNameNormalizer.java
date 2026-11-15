package com.pdcm.suggestion;

import java.util.Locale;

/**
 * Normalizes destination names for duplicate detection: trim, lower-case,
 * strip punctuation, collapse whitespace.
 */
public final class SuggestionNameNormalizer {

	private SuggestionNameNormalizer() {
	}

	public static String normalize(String raw) {
		if (raw == null) {
			return "";
		}
		String lower = raw.trim().toLowerCase(Locale.ROOT);
		String stripped = lower.replaceAll("[^\\p{L}\\p{N}\\s]+", " ");
		return stripped.replaceAll("\\s+", " ").trim();
	}

	public static String slugify(String raw) {
		String normalized = normalize(raw);
		if (normalized.isBlank()) {
			return "destination";
		}
		String slug = normalized.replace(' ', '-');
		if (slug.length() > 200) {
			slug = slug.substring(0, 200).replaceAll("-+$", "");
		}
		return slug.isBlank() ? "destination" : slug;
	}
}
