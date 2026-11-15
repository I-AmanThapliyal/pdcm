import type { CrowdStatus, Destination, TrafficStatus } from './types'

export type DiscoverView = 'map' | 'list'

export function destinationPlaceLine(destination: Destination): string {
  const place = destination.city?.trim() || destination.region
  return `${place} · ${destination.category.replaceAll('_', ' ')}`
}

/**
 * Status copy for the accessible list.
 * Initial load may show a loading line; background refresh never replaces existing rows with a loading state.
 */
export function discoverListMessage(options: {
  initialLoading: boolean
  refreshing: boolean
  error: string | null
  count: number
}): string | null {
  if (options.initialLoading && options.count === 0) {
    return 'Loading destinations in this view…'
  }
  if (options.error && options.count === 0) {
    return options.error
  }
  if (options.count === 0 && !options.refreshing) {
    return 'No destinations match these filters in the current map area.'
  }
  return null
}

export function discoverRefreshHint(options: {
  refreshing: boolean
  error: string | null
  count: number
}): string | null {
  if (options.refreshing && options.count > 0) {
    return 'Updating destinations for this view…'
  }
  if (options.error && options.count > 0) {
    return options.error
  }
  return null
}

/** Keep the selected place even when a bounds refresh omits it from the current result set. */
export function resolveSelectedDestination(
  previous: Destination | null,
  selectedId: number | null,
  destinations: Destination[],
): Destination | null {
  if (selectedId == null) return null
  const fromList = destinations.find((item) => item.id === selectedId)
  if (fromList) return fromList
  if (previous?.id === selectedId) return previous
  return null
}

export function unknownIsNotQuiet(status: CrowdStatus | TrafficStatus): boolean {
  return status === 'UNKNOWN'
}
