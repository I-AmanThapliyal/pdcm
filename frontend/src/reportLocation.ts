export type ClientLocationSample = {
  latitude: number
  longitude: number
  accuracyMeters: number | null
}

export type LocationCaptureResult =
  | { status: 'skipped' }
  | { status: 'captured'; location: ClientLocationSample }
  | { status: 'unavailable'; message: string }

export function verificationMessage(
  verificationStatus: string,
  verificationReason: string | null | undefined,
  locationCapture: LocationCaptureResult,
): string {
  if (verificationStatus === 'LOCATION_VERIFIED') {
    return 'Report saved with location verification. Proximity near the place was checked — that is not proof the report is genuine. Only location-verified reports feed crowd and community traffic after the cutover.'
  }
  if (locationCapture.status === 'unavailable') {
    return `Report saved as pending (${verificationReason ?? 'LOCATION_UNAVAILABLE'}). ${locationCapture.message} It will not count toward verified crowd/traffic until location can be verified.`
  }
  if (verificationReason === 'NO_CONSENT') {
    return 'Report saved as pending without location sharing. It will not count toward verified crowd/traffic. You can share location next time for a proximity check.'
  }
  if (verificationReason === 'OUTSIDE_GEOFENCE') {
    return 'Report saved as pending: you did not appear close enough to this place for location verification. Proximity is a signal, not absolute proof.'
  }
  if (verificationReason === 'ACCURACY_TOO_LOW') {
    return 'Report saved as pending: GPS accuracy was too coarse for location verification.'
  }
  if (verificationReason === 'SUSPICIOUS_COORDINATES' || verificationStatus === 'FLAGGED') {
    return 'Report saved but flagged for review due to unusual coordinates. It will not count toward verified crowd/traffic.'
  }
  return `Report saved with status ${verificationStatus}${verificationReason ? ` (${verificationReason})` : ''}. Only LOCATION_VERIFIED reports feed verified community aggregation.`
}

export async function captureClientLocation(consent: boolean): Promise<LocationCaptureResult> {
  if (!consent) {
    return { status: 'skipped' }
  }
  if (typeof navigator === 'undefined' || !navigator.geolocation) {
    return {
      status: 'unavailable',
      message: 'Location is unavailable in this browser.',
    }
  }

  try {
    const position = await new Promise<GeolocationPosition>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 12_000,
        maximumAge: 30_000,
      })
    })
    return {
      status: 'captured',
      location: {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
        accuracyMeters:
          typeof position.coords.accuracy === 'number' && Number.isFinite(position.coords.accuracy)
            ? position.coords.accuracy
            : null,
      },
    }
  } catch (error) {
    const code = error && typeof error === 'object' && 'code' in error ? Number(error.code) : null
    if (code === 1) {
      return { status: 'unavailable', message: 'Location permission was denied.' }
    }
    if (code === 3) {
      return { status: 'unavailable', message: 'Location request timed out.' }
    }
    return { status: 'unavailable', message: 'Current location could not be read.' }
  }
}
