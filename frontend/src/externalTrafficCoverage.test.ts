import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  externalCoverageDetail,
  externalCoverageLabel,
  externalCoverageVisibility,
} from './externalTrafficCoverage.ts'

describe('externalCoverageVisibility', () => {
  it('marks non-pilot destinations unavailable without implying clear roads', () => {
    assert.equal(
      externalCoverageVisibility({
        pilotEnabled: false,
        coverageAvailable: false,
        status: 'UNAVAILABLE',
        freshness: 'NONE',
      }),
      'UNAVAILABLE',
    )
  })

  it('marks usable fresh pilot coverage as supported (single-road only)', () => {
    assert.equal(
      externalCoverageVisibility({
        pilotEnabled: true,
        coverageAvailable: true,
        status: 'MODERATE',
        freshness: 'FRESH',
      }),
      'SUPPORTED',
    )
  })

  it('marks expired cached usable samples as stale', () => {
    assert.equal(
      externalCoverageVisibility({
        pilotEnabled: true,
        coverageAvailable: true,
        status: 'HEAVY',
        freshness: 'STALE',
      }),
      'STALE',
    )
  })

  it('treats unknown geometry as unavailable coverage', () => {
    assert.equal(
      externalCoverageVisibility({
        pilotEnabled: true,
        coverageAvailable: false,
        status: 'UNKNOWN',
        freshness: 'FRESH',
      }),
      'UNAVAILABLE',
    )
  })
})

describe('externalCoverage labels', () => {
  it('uses Supported / Unavailable / Stale wording', () => {
    assert.equal(externalCoverageLabel('SUPPORTED'), 'Supported')
    assert.equal(externalCoverageLabel('UNAVAILABLE'), 'Unavailable')
    assert.equal(externalCoverageLabel('STALE'), 'Stale')
  })

  it('keeps single-road wording for supported coverage', () => {
    const detail = externalCoverageDetail({
      visibility: 'SUPPORTED',
      pilotEnabled: true,
      coverageNote: 'One nearby approach road segment only — not destination-wide coverage.',
      message: null,
    })
    assert.match(detail, /not destination-wide/)
  })
})
