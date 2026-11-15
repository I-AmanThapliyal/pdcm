import type { Dashboard, DestinationSnapshot } from './types'

export type DashboardViewState =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; dashboard: Dashboard }

export function quieterEmptyCopy(items: DestinationSnapshot[]): string | null {
  if (items.length > 0) return null
  return 'No destination has a known live crowd score yet, so nothing here is called quieter. Unknown is not quiet.'
}

export function recentReportsEmptyCopy(count: number): string | null {
  if (count > 0) return null
  return 'No traveler reports yet. Missing reports mean unknown conditions — not an empty venue.'
}

export function popularUnknownNeedsReport(item: DestinationSnapshot): boolean {
  return item.crowd.status === 'UNKNOWN'
}
