function osmTileUrlFromEnv(): string {
  try {
    const value = import.meta.env?.VITE_OSM_TILE_URL
    if (typeof value === 'string' && value.trim()) return value.trim()
  } catch {
    // Node unit tests do not provide Vite's import.meta.env
  }
  return 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
}

/** Configurable OSM raster tile endpoint for ordinary interactive viewing only. */
export const OSM_TILE_URL = osmTileUrlFromEnv()

/**
 * Community OSM tile servers are best-effort with no availability guarantee.
 * Honor normal browser caching/referrers; do not prefetch, proxy, or bulk-download tiles.
 * Policy: https://operations.osmfoundation.org/policies/tiles/
 */
export const OSM_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
