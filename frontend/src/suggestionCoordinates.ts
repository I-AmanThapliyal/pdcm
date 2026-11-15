/** Soft India bounding box — mirrors backend DestinationSuggestionService. */
export const INDIA_LAT_MIN = 6.0
export const INDIA_LAT_MAX = 37.5
export const INDIA_LON_MIN = 68.0
export const INDIA_LON_MAX = 98.0

export type CoordinateParseResult =
  | { ok: true; latitude: number; longitude: number }
  | { ok: false; message: string }

/**
 * Parse and validate admin-entered suggestion coordinates (WGS84 + soft India).
 * Rejects blank, NaN, out-of-range, null-island, and out-of-soft-India values.
 */
export function parseSuggestionCoordinates(
  latitudeRaw: string,
  longitudeRaw: string,
): CoordinateParseResult {
  const latTrim = latitudeRaw.trim()
  const lonTrim = longitudeRaw.trim()
  if (!latTrim || !lonTrim) {
    return { ok: false, message: 'Latitude and longitude are required.' }
  }
  const latitude = Number(latTrim)
  const longitude = Number(lonTrim)
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
    return { ok: false, message: 'Latitude and longitude must be valid numbers.' }
  }
  if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
    return { ok: false, message: 'Latitude must be -90…90 and longitude -180…180.' }
  }
  if (Math.abs(latitude) < 1e-6 && Math.abs(longitude) < 1e-6) {
    return { ok: false, message: 'Coordinates look invalid (null island).' }
  }
  if (
    latitude < INDIA_LAT_MIN ||
    latitude > INDIA_LAT_MAX ||
    longitude < INDIA_LON_MIN ||
    longitude > INDIA_LON_MAX
  ) {
    return { ok: false, message: 'Coordinates must fall within the soft India bounding box.' }
  }
  return { ok: true, latitude, longitude }
}
