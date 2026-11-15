import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  popularUnknownNeedsReport,
  quieterEmptyCopy,
  recentReportsEmptyCopy,
} from './dashboardStates.ts'
import type { DestinationSnapshot } from './types.ts'

function snapshot(status: DestinationSnapshot['crowd']['status']): DestinationSnapshot {
  return {
    destination: {
      id: 1,
      name: 'Test',
      slug: 'test',
      description: '',
      category: 'HISTORICAL',
      region: 'Rajasthan',
      city: null,
      latitude: 0,
      longitude: 0,
      crowdSensitivity: 'MEDIUM',
      tags: [],
    },
    crowd: {
      destinationId: 1,
      status,
      score: status === 'UNKNOWN' ? null : 1.2,
      confidence: status === 'UNKNOWN' ? 'NONE' : 'LOW',
      contributingReports: status === 'UNKNOWN' ? 0 : 1,
      lastObservedAt: status === 'UNKNOWN' ? null : '2026-11-15T11:00:00Z',
      asOf: '2026-11-15T12:00:00Z',
      source: 'USER_REPORTS',
    },
    traffic: {
      destinationId: 1,
      status: 'UNKNOWN',
      confidence: 'NONE',
      contributingReports: 0,
      lastObservedAt: null,
      asOf: '2026-11-15T12:00:00Z',
      source: 'USER_REPORTS',
    },
    recentReportCount: 0,
    verifiedRecentReportCount: 0,
    forecastQuality: {
      targetDate: '2026-11-16',
      forecastStatus: 'INSUFFICIENT_DATA',
      estimatedIntensity: null,
      estimatedStatus: null,
      confidence: 'NONE',
      sampleSize: 0,
      message: 'Not enough',
    },
  }
}

describe('dashboard empty and unknown states', () => {
  it('explains empty quieter list without calling places quiet', () => {
    const copy = quieterEmptyCopy([])
    assert.ok(copy)
    assert.match(copy, /not quiet/i)
    assert.equal(quieterEmptyCopy([snapshot('QUIET')]), null)
  })

  it('explains empty recent reports without implying no crowd', () => {
    const copy = recentReportsEmptyCopy(0)
    assert.ok(copy)
    assert.match(copy, /unknown/i)
    assert.equal(recentReportsEmptyCopy(2), null)
  })

  it('flags unknown crowd for report CTA', () => {
    assert.equal(popularUnknownNeedsReport(snapshot('UNKNOWN')), true)
    assert.equal(popularUnknownNeedsReport(snapshot('QUIET')), false)
  })
})
