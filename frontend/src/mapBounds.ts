/** India-wide starting view (center + zoom). */
export const INDIA_MAP_CENTER: [number, number] = [22.5, 79]
export const INDIA_MAP_ZOOM = 5

/** Initial query window that fits India within the API max span. */
export const INDIA_BOUNDS = {
  south: 6.5,
  west: 68,
  north: 35.5,
  east: 97,
} as const

export const MAP_MAX_SPAN_DEGREES = 40
export const MAP_RESULT_LIMIT = 100

/** Ignore tiny Leaflet pan jitter / float noise between consecutive bounds. */
export const BOUNDS_EPSILON_DEGREES = 0.0005

export interface MapBounds {
  south: number
  west: number
  north: number
  east: number
}

/** Shrink oversized bounds around their center so the API span limit is respected. */
export function clampBoundsToMaxSpan(bounds: MapBounds, maxSpan = MAP_MAX_SPAN_DEGREES): MapBounds {
  let { south, west, north, east } = bounds
  const latSpan = north - south
  if (latSpan > maxSpan) {
    const mid = (south + north) / 2
    south = mid - maxSpan / 2
    north = mid + maxSpan / 2
  }
  const lonSpan = east - west
  if (lonSpan > maxSpan) {
    const mid = (west + east) / 2
    west = mid - maxSpan / 2
    east = mid + maxSpan / 2
  }
  return { south, west, north, east }
}

export function boundsNearlyEqual(
  left: MapBounds,
  right: MapBounds,
  epsilon = BOUNDS_EPSILON_DEGREES,
): boolean {
  return (
    Math.abs(left.south - right.south) <= epsilon
    && Math.abs(left.west - right.west) <= epsilon
    && Math.abs(left.north - right.north) <= epsilon
    && Math.abs(left.east - right.east) <= epsilon
  )
}

export function buildMapQuery(params: {
  bounds: MapBounds
  q?: string
  region?: string
  category?: string
  limit?: number
}): URLSearchParams {
  const bounds = clampBoundsToMaxSpan(params.bounds)
  const query = new URLSearchParams()
  query.set('south', String(bounds.south))
  query.set('west', String(bounds.west))
  query.set('north', String(bounds.north))
  query.set('east', String(bounds.east))
  query.set('limit', String(params.limit ?? MAP_RESULT_LIMIT))
  if (params.q) query.set('q', params.q)
  if (params.region) query.set('region', params.region)
  if (params.category) query.set('category', params.category)
  return query
}

export function defaultIndiaBounds(): MapBounds {
  return { ...INDIA_BOUNDS }
}
