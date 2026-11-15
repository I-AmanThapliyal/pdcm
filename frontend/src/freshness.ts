/**
 * Human-readable observation freshness from real timestamps + configured window.
 * Never invents timestamps; missing lastObservedAt → no recent reports.
 */

export type FreshnessKind = 'NONE' | 'UPDATED' | 'REPORTED'

export interface FreshnessCopy {
  kind: FreshnessKind
  label: string
}

function ageParts(lastObservedAt: string, asOf: string): { totalMinutes: number; totalHours: number } {
  const ageMs = Math.max(0, new Date(asOf).getTime() - new Date(lastObservedAt).getTime())
  const totalMinutes = Math.floor(ageMs / 60_000)
  const totalHours = Math.floor(totalMinutes / 60)
  return { totalMinutes, totalHours }
}

function formatAge(totalMinutes: number, totalHours: number): string {
  if (totalMinutes < 1) return 'just now'
  if (totalMinutes < 60) return `${totalMinutes}m ago`
  if (totalHours < 24) return `${totalHours}h ago`
  const days = Math.floor(totalHours / 24)
  return `${days}d ago`
}

/**
 * Crowd / community traffic observation freshness.
 * Within ~1h → "Updated …"; otherwise still inside the live window → "Last reported …".
 */
export function observationFreshness(
  lastObservedAt: string | null | undefined,
  asOf: string,
  windowHours = 6,
): FreshnessCopy {
  if (!lastObservedAt) {
    return { kind: 'NONE', label: 'No recent reports' }
  }
  const { totalMinutes, totalHours } = ageParts(lastObservedAt, asOf)
  if (totalHours >= windowHours) {
    // Outside the live aggregation window — treat as no current observation.
    return { kind: 'NONE', label: 'No recent reports' }
  }
  const age = formatAge(totalMinutes, totalHours)
  if (totalMinutes < 60) {
    return { kind: 'UPDATED', label: `Updated ${age}` }
  }
  return { kind: 'REPORTED', label: `Last reported ${age}` }
}

export function sourceLabel(source: string | null | undefined): string {
  if (!source || source === 'USER_REPORTS') return 'Traveler reports'
  return source
}

export function providerAvailabilityCopy(provider: {
  configured: boolean
  status: string
  scheduleEnabled: boolean
  pilotSlugs: string[]
  cacheTtlMinutes?: number
}): string {
  if (!provider.configured || provider.status === 'DISABLED') {
    return 'External road traffic unavailable (provider not configured). This is not “clear roads.”'
  }
  const pilot = provider.pilotSlugs.length
  const ttl = provider.cacheTtlMinutes ?? 180
  const schedule = provider.scheduleEnabled
    ? 'scheduled refresh on'
    : `on-demand only; ${ttl}m shared cache; schedule off`
  if (provider.status === 'UP') {
    return `External road traffic available for ${pilot} pilot place${pilot === 1 ? '' : 's'} (${schedule}).`
  }
  if (provider.status === 'DEGRADED') {
    return `External road traffic degraded for the ${pilot}-place pilot (${schedule}).`
  }
  return `External road traffic unavailable right now (${provider.status.toLowerCase()}). Not the same as light traffic.`
}

export function forecastQualityCopy(forecast: {
  forecastStatus: 'INSUFFICIENT_DATA' | 'ESTIMATE'
  estimatedIntensity: number | null
  estimatedStatus: string | null
  confidence: string
  sampleSize: number
  message: string
  targetDate: string
}): string {
  if (forecast.forecastStatus === 'INSUFFICIENT_DATA') {
    return `Tomorrow (${forecast.targetDate}): insufficient history — no numeric forecast (sample ${forecast.sampleSize}/5).`
  }
  return `Tomorrow (${forecast.targetDate}): estimate ${forecast.estimatedIntensity} (${forecast.estimatedStatus}), ${forecast.confidence.toLowerCase()} confidence from ${forecast.sampleSize} same-weekday reports — not a live observation.`
}

export function unknownCrowdCopy(): string {
  return 'No location-verified crowd reports in the live window. Unknown is not quiet.'
}

export function unknownTrafficCopy(): string {
  return 'No location-verified community traffic reports in the live window. Unknown is not light traffic.'
}
