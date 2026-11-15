package com.pdcm.suggestion;

import java.time.Instant;

public record SuggestionEventResponse(
		Long id,
		Long suggestionId,
		Long actorUserId,
		SuggestionEventAction action,
		SuggestionStatus fromStatus,
		SuggestionStatus toStatus,
		String note,
		Instant createdAt
) {
	public static SuggestionEventResponse from(DestinationSuggestionEvent event) {
		return new SuggestionEventResponse(
				event.getId(),
				event.getSuggestion().getId(),
				event.getActor().getId(),
				event.getAction(),
				event.getFromStatus(),
				event.getToStatus(),
				event.getNote(),
				event.getCreatedAt());
	}
}
