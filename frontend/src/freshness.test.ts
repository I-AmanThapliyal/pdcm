import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  forecastQualityCopy,
  observationFreshness,
  providerAvailabilityCopy,
  sourceLabel,
  unknownCrowdCopy,
  unknownTrafficCopy,
} from './freshness.ts'

describe('observationFreshness', () => {
  const asOf = '2026-11-15T12:00:00Z'

  it('treats missing timestamp as no recent reports', () => {
    assert.deepEqual(observationFreshness(null, asOf), {
      kind: 'NONE',
      label: 'No recent reports',
    })
  })

  it('formats a recent update from real timestamps', () => {
    assert.deepEqual(observationFreshness('2026-11-15T11:45:00Z', asOf), {
      kind: 'UPDATED',
      label: 'Updated 15m ago',
    })
  })

  it('formats an older in-window report without inventing time', () => {
    assert.deepEqual(observationFreshness('2026-11-15T10:00:00Z', asOf), {
      kind: 'REPORTED',
      label: 'Last reported 2h ago',
    })
  })

  it('does not present stale-outside-window as current', () => {
    assert.deepEqual(observationFreshness('2026-11-15T05:00:00Z', asOf, 6), {
      kind: 'NONE',
      label: 'No recent reports',
    })
  })
})

describe('providerAvailabilityCopy', () => {
  it('says unavailable when provider is not configured — not clear roads', () => {
    const copy = providerAvailabilityCopy({
      configured: false,
      status: 'DISABLED',
      scheduleEnabled: false,
      pilotSlugs: [],
    })
    assert.match(copy, /unavailable/i)
    assert.match(copy, /not configured/i)
    assert.match(copy, /not .*clear roads/i)
  })

  it('describes available pilot without merging into community traffic', () => {
    const copy = providerAvailabilityCopy({
      configured: true,
      status: 'UP',
      scheduleEnabled: false,
      pilotSlugs: ['a', 'b', 'c', 'd', 'e'],
    })
    assert.match(copy, /pilot/)
    assert.match(copy, /on-demand/)
  })
})

describe('forecastQualityCopy', () => {
  it('refuses numeric forecast when history is insufficient', () => {
    const copy = forecastQualityCopy({
      forecastStatus: 'INSUFFICIENT_DATA',
      estimatedIntensity: null,
      estimatedStatus: null,
      confidence: 'NONE',
      sampleSize: 2,
      message: 'Not enough',
      targetDate: '2026-11-16',
    })
    assert.match(copy, /insufficient history/i)
    assert.match(copy, /no numeric forecast/i)
  })

  it('labels estimates as not live observations', () => {
    const copy = forecastQualityCopy({
      forecastStatus: 'ESTIMATE',
      estimatedIntensity: 2.0,
      estimatedStatus: 'MODERATE',
      confidence: 'LOW',
      sampleSize: 5,
      message: 'Estimate',
      targetDate: '2026-11-16',
    })
    assert.match(copy, /estimate 2/)
    assert.match(copy, /not a live observation/i)
  })
})

describe('honesty copy', () => {
  it('keeps unknown distinct from quiet or light traffic', () => {
    assert.match(unknownCrowdCopy(), /not quiet/i)
    assert.match(unknownTrafficCopy(), /not light traffic/i)
    assert.equal(sourceLabel('USER_REPORTS'), 'Traveler reports')
  })
})
