package com.pdcm.report;

/**
 * Public-safe trust signal for a report. Never includes email or other PII.
 */
public enum ReportTrustLabel {
	LOCATION_VERIFIED_SIGNAL,
	PENDING_REVIEW,
	FLAGGED_FOR_REVIEW,
	REJECTED,
	LEGACY_UNVERIFIED;

	public static ReportTrustLabel from(VerificationStatus status) {
		return switch (status) {
			case LOCATION_VERIFIED -> LOCATION_VERIFIED_SIGNAL;
			case PENDING -> PENDING_REVIEW;
			case FLAGGED -> FLAGGED_FOR_REVIEW;
			case REJECTED -> REJECTED;
			case LEGACY_UNVERIFIED -> LEGACY_UNVERIFIED;
		};
	}
}
