import type { ReportTrustLabel, VerificationStatus } from './types'

/** Public-safe wording for report trust — no personal information. */
export function trustLabelText(label: ReportTrustLabel | undefined, status?: VerificationStatus): string {
  const key = label ?? (status ? defaultTrustLabel(status) : undefined)
  switch (key) {
    case 'LOCATION_VERIFIED_SIGNAL':
      return 'Location-verified signal'
    case 'PENDING_REVIEW':
      return 'Pending review'
    case 'FLAGGED_FOR_REVIEW':
      return 'Flagged for review'
    case 'REJECTED':
      return 'Rejected'
    case 'LEGACY_UNVERIFIED':
      return 'Legacy unverified'
    default:
      return 'Unknown trust'
  }
}

function defaultTrustLabel(status: VerificationStatus): ReportTrustLabel {
  switch (status) {
    case 'LOCATION_VERIFIED':
      return 'LOCATION_VERIFIED_SIGNAL'
    case 'PENDING':
      return 'PENDING_REVIEW'
    case 'FLAGGED':
      return 'FLAGGED_FOR_REVIEW'
    case 'REJECTED':
      return 'REJECTED'
    case 'LEGACY_UNVERIFIED':
      return 'LEGACY_UNVERIFIED'
  }
}
