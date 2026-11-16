import type { ExternalFreshness, ExternalTrafficStatus } from './types'

export type ExternalCoverageVisibility = 'SUPPORTED' | 'UNAVAILABLE' | 'STALE'

/**
 * Destination-level external road coverage visibility.
 * Provider UP alone is never destination-wide coverage — only a nearby single-road sample.
 */
export function externalCoverageVisibility(input: {
  pilotEnabled: boolean
  coverageAvailable: boolean
  status: ExternalTrafficStatus
  freshness: ExternalFreshness
}): ExternalCoverageVisibility {
  if (!input.pilotEnabled) {
    return 'UNAVAILABLE'
  }
  if (input.status === 'UNAVAILABLE') {
    return 'UNAVAILABLE'
  }
  if (input.freshness === 'STALE' && input.coverageAvailable) {
    return 'STALE'
  }
  if (input.coverageAvailable && input.status !== 'UNKNOWN') {
    return 'SUPPORTED'
  }
  if (input.status === 'UNKNOWN') {
    return 'UNAVAILABLE'
  }
  return 'UNAVAILABLE'
}

export function externalCoverageLabel(visibility: ExternalCoverageVisibility): string {
  switch (visibility) {
    case 'SUPPORTED':
      return 'Supported'
    case 'STALE':
      return 'Stale'
    default:
      return 'Unavailable'
  }
}

export function externalCoverageDetail(input: {
  visibility: ExternalCoverageVisibility
  pilotEnabled: boolean
  coverageNote: string | null
  message: string | null
}): string {
  if (!input.pilotEnabled) {
    return 'Outside the TomTom external-traffic pilot for this destination.'
  }
  if (input.visibility === 'SUPPORTED') {
    return input.coverageNote ?? 'One nearby approach road segment only — not destination-wide coverage.'
  }
  if (input.visibility === 'STALE') {
    return `Stale cached sample. ${input.coverageNote ?? 'One nearby approach road segment only — not destination-wide coverage.'}`
  }
  return input.message ?? 'No suitable nearby road sample available.'
}
