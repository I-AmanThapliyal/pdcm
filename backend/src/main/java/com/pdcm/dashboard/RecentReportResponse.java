package com.pdcm.dashboard;

import java.time.Instant;

import com.pdcm.report.ReportTrustLabel;
import com.pdcm.report.TrafficCondition;
import com.pdcm.report.VerificationReason;
import com.pdcm.report.VerificationStatus;
import com.pdcm.report.VisitReport;

/**
 * Public recent-report row. No email/display name; trustLabel explains report quality.
 */
public record RecentReportResponse(
		Long id,
		Long destinationId,
		String destinationName,
		int crowdIntensity,
		TrafficCondition trafficCondition,
		Instant observedAt,
		Instant createdAt,
		VerificationStatus verificationStatus,
		VerificationReason verificationReason,
		ReportTrustLabel trustLabel
) {
	public static RecentReportResponse from(VisitReport report) {
		return new RecentReportResponse(
				report.getId(),
				report.getDestination().getId(),
				report.getDestination().getName(),
				report.getCrowdIntensity(),
				report.getTrafficCondition(),
				report.getObservedAt(),
				report.getCreatedAt(),
				report.getVerificationStatus(),
				report.getVerificationReason(),
				ReportTrustLabel.from(report.getVerificationStatus()));
	}
}
