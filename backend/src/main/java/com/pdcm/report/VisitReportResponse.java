package com.pdcm.report;

import java.time.Instant;

/**
 * Public report DTO. Precise client coordinates are intentionally omitted.
 * trustLabel is a high-level signal without contributor PII.
 */
public record VisitReportResponse(
		Long id,
		Long destinationId,
		Long userId,
		int crowdIntensity,
		TrafficCondition trafficCondition,
		Integer waitMinutes,
		String note,
		Instant observedAt,
		Instant createdAt,
		VerificationStatus verificationStatus,
		VerificationReason verificationReason,
		ReportTrustLabel trustLabel
) {
	public static VisitReportResponse from(VisitReport report) {
		return new VisitReportResponse(
				report.getId(),
				report.getDestination().getId(),
				report.getUser().getId(),
				report.getCrowdIntensity(),
				report.getTrafficCondition(),
				report.getWaitMinutes(),
				report.getNote(),
				report.getObservedAt(),
				report.getCreatedAt(),
				report.getVerificationStatus(),
				report.getVerificationReason(),
				ReportTrustLabel.from(report.getVerificationStatus()));
	}
}
