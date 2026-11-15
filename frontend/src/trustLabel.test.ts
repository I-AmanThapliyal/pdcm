import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { trustLabelText } from './trustLabel.ts'

describe('trustLabelText', () => {
  it('explains location-verified without implying personal proof', () => {
    assert.equal(trustLabelText('LOCATION_VERIFIED_SIGNAL'), 'Location-verified signal')
  })

  it('maps flagged and pending without exposing PII', () => {
    assert.equal(trustLabelText('FLAGGED_FOR_REVIEW'), 'Flagged for review')
    assert.equal(trustLabelText('PENDING_REVIEW'), 'Pending review')
    assert.equal(trustLabelText(undefined, 'REJECTED'), 'Rejected')
  })
})
