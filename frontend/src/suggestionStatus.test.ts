import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { duplicateReasonText, suggestionStatusText } from './suggestionStatus.ts'

describe('suggestionStatusText', () => {
  it('explains statuses without exposing contributor identity', () => {
    assert.equal(suggestionStatusText('SUBMITTED'), 'Submitted — awaiting review')
    assert.equal(suggestionStatusText('APPROVED'), 'Approved — added to catalogue')
    assert.equal(suggestionStatusText('REJECTED'), 'Rejected')
  })
})

describe('duplicateReasonText', () => {
  it('maps match reasons for review UI', () => {
    assert.equal(duplicateReasonText('SAME_NAME_AND_REGION'), 'Same name and region')
    assert.equal(duplicateReasonText('PROXIMITY_AND_CONTEXT'), 'Nearby with matching context')
  })
})
