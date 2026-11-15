import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { verificationMessage } from './reportLocation.ts'

describe('verificationMessage', () => {
  it('explains location verified is proximity only', () => {
    const message = verificationMessage('LOCATION_VERIFIED', 'WITHIN_GEOFENCE', {
      status: 'captured',
      location: { latitude: 1, longitude: 2, accuracyMeters: 10 },
    })
    assert.match(message, /location verification/i)
    assert.match(message, /not proof/i)
  })

  it('explains pending without consent', () => {
    const message = verificationMessage('PENDING', 'NO_CONSENT', { status: 'skipped' })
    assert.match(message, /pending/i)
    assert.match(message, /will not count/i)
  })

  it('explains permission denial', () => {
    const message = verificationMessage('PENDING', 'LOCATION_UNAVAILABLE', {
      status: 'unavailable',
      message: 'Location permission was denied.',
    })
    assert.match(message, /Location permission was denied/)
    assert.match(message, /pending/i)
  })
})
