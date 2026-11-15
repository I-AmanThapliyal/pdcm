package com.pdcm.report;

/**
 * Report verification for community aggregation.
 * Only {@link #LOCATION_VERIFIED} contributes after the GPS cutover.
 * {@link #LOCATION_VERIFIED} means proximity at submit time — not proof the report is genuine.
 */
public enum VerificationStatus {
	LEGACY_UNVERIFIED,
	PENDING,
	LOCATION_VERIFIED,
	FLAGGED,
	REJECTED
}
