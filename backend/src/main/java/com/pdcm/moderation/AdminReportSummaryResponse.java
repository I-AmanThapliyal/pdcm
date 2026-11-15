package com.pdcm.moderation;

import java.time.Instant;

import com.pdcm.report.ReportTrustLabel;
import com.pdcm.report.TrafficCondition;
import com.pdcm.report.VerificationReason;
import com.pdcm.report.VerificationStatus;
import com.pdcm.report.VisitReport;

/**
 * Admin list/detail fields without raw client coordinates.
 * Distance and verification metadata are safe; coords are never included here.
 */
public record AdminReportSummaryResponse(
		Long id,
		Long destinationId,
		String destinationName,
		Long contributorUserId,
		int crowdIntensity,
		TrafficCondition trafficCondition,
		Integer waitMinutes,
		String note,
		Instant observedAt,
		Instant createdAt,
		VerificationStatus verificationStatus,
		VerificationReason verificationReason,
		ReportTrustLabel trustLabel,
		Double distanceToDestinationMeters,
		boolean coordinatesRetained,
		Instant locationConsentAt,
		Instant locationPurgedAt
) {
	public static AdminReportSummaryResponse from(VisitReport report) {
		return new AdminReportSummaryResponse(
				report.getId(),
				report.getDestination().getId(),
				report.getDestination().getName(),
				report.getUser().getId(),
				report.getCrowdIntensity(),
				report.getTrafficCondition(),
				report.getWaitMinutes(),
				report.getNote(),
				report.getObservedAt(),
				report.getCreatedAt(),
				report.getVerificationStatus(),
				report.getVerificationReason(),
				ReportTrustLabel.from(report.getVerificationStatus()),
				report.getDistanceToDestinationMeters(),
				report.hasRetainedClientCoordinates(),
				report.getLocationConsentAt(),
				report.getLocationPurgedAt());
	}
}
