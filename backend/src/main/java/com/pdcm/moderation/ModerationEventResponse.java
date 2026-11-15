package com.pdcm.moderation;

import java.time.Instant;

import com.pdcm.report.VerificationStatus;

public record ModerationEventResponse(
		Long id,
		Long reportId,
		Long actorUserId,
		VerificationStatus fromStatus,
		VerificationStatus toStatus,
		String note,
		Instant createdAt
) {
	public static ModerationEventResponse from(ReportModerationEvent event) {
		return new ModerationEventResponse(
				event.getId(),
				event.getReport().getId(),
				event.getActor().getId(),
				event.getFromStatus(),
				event.getToStatus(),
				event.getNote(),
				event.getCreatedAt());
	}
}
