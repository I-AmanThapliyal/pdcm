package com.pdcm.report;

/**
 * Machine-readable reason for {@link VerificationStatus}. Safe for public APIs
 * (no coordinates).
 */
public enum VerificationReason {
	PRE_CUTOVER,
	NO_CONSENT,
	LOCATION_UNAVAILABLE,
	INVALID_COORDINATES,
	SUSPICIOUS_COORDINATES,
	ACCURACY_TOO_LOW,
	OUTSIDE_GEOFENCE,
	WITHIN_GEOFENCE,
	/** Admin explicitly confirmed location eligibility after review. */
	ADMIN_VERIFIED,
	/** Admin rejected the report (abuse or ineligible). */
	ADMIN_REJECTED,
	/** Admin flagged a previously verified report for abuse/review. */
	ADMIN_FLAGGED
}
