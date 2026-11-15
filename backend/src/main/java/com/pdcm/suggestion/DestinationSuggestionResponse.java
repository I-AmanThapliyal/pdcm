package com.pdcm.suggestion;

import java.time.Instant;
import java.util.List;

import com.pdcm.destination.DestinationCategory;

/**
 * User-facing suggestion view. Never includes submitter email.
 * Admin detail adds contributorUserId, possibleDuplicates, and audit history.
 */
public record DestinationSuggestionResponse(
		Long id,
		String name,
		String city,
		String region,
		DestinationCategory category,
		String description,
		Double latitude,
		Double longitude,
		String referenceUrl,
		SuggestionStatus status,
		String adminNote,
		Long createdDestinationId,
		boolean possibleDuplicate,
		List<PossibleDuplicateResponse> possibleDuplicates,
		Long contributorUserId,
		List<SuggestionEventResponse> auditHistory,
		Instant createdAt,
		Instant updatedAt
) {
	public static DestinationSuggestionResponse forOwner(
			DestinationSuggestion suggestion,
			List<PossibleDuplicateResponse> duplicates) {
		return base(suggestion, duplicates, null, List.of());
	}

	public static DestinationSuggestionResponse forAdmin(
			DestinationSuggestion suggestion,
			List<PossibleDuplicateResponse> duplicates,
			List<SuggestionEventResponse> history) {
		return base(suggestion, duplicates, suggestion.getSubmitter().getId(), history);
	}

	private static DestinationSuggestionResponse base(
			DestinationSuggestion suggestion,
			List<PossibleDuplicateResponse> duplicates,
			Long contributorUserId,
			List<SuggestionEventResponse> history) {
		List<PossibleDuplicateResponse> safeDuplicates = duplicates == null ? List.of() : List.copyOf(duplicates);
		Long createdId = suggestion.getCreatedDestination() == null
				? null
				: suggestion.getCreatedDestination().getId();
		return new DestinationSuggestionResponse(
				suggestion.getId(),
				suggestion.getName(),
				suggestion.getCity(),
				suggestion.getRegion(),
				suggestion.getCategory(),
				suggestion.getDescription(),
				suggestion.getLatitude(),
				suggestion.getLongitude(),
				suggestion.getReferenceUrl(),
				suggestion.getStatus(),
				suggestion.getAdminNote(),
				createdId,
				!safeDuplicates.isEmpty(),
				safeDuplicates,
				contributorUserId,
				history == null ? List.of() : List.copyOf(history),
				suggestion.getCreatedAt(),
				suggestion.getUpdatedAt());
	}
}
