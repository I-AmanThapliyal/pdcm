package com.pdcm.moderation;

import java.util.List;

/**
 * Admin detail. Raw coordinates appear only when still retained and explicitly
 * requested via includeCoordinates=true. Never log these values.
 */
public record AdminReportDetailResponse(
		AdminReportSummaryResponse report,
		AdminRetainedCoordinates retainedCoordinates,
		List<ModerationEventResponse> moderationHistory
) {
	public record AdminRetainedCoordinates(
			double latitude,
			double longitude,
			Double accuracyMeters
	) {
	}
}
