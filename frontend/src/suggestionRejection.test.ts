import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  DEFAULT_DUPLICATE_REJECTION_REASON,
  REJECTION_REASON_MAX_LENGTH,
  defaultRejectionReason,
  parseRejectionReason,
} from './suggestionRejection.ts'

describe('parseRejectionReason', () => {
  it('requires a non-blank reason', () => {
    assert.equal(parseRejectionReason(null).ok, false)
    assert.equal(parseRejectionReason('').ok, false)
    assert.equal(parseRejectionReason('   ').ok, false)
  })

  it('trims and accepts a valid reason', () => {
    const parsed = parseRejectionReason('  Duplicate of catalogue entry  ')
    assert.equal(parsed.ok, true)
    if (parsed.ok) {
      assert.equal(parsed.reason, 'Duplicate of catalogue entry')
    }
  })

  it('rejects reasons longer than 500 characters', () => {
    const tooLong = 'x'.repeat(REJECTION_REASON_MAX_LENGTH + 1)
    const parsed = parseRejectionReason(tooLong)
    assert.equal(parsed.ok, false)
    if (!parsed.ok) {
      assert.match(parsed.message, /500/)
    }
  })
})

describe('defaultRejectionReason', () => {
  it('pre-fills the duplicate catalogue message when duplicates are known', () => {
    assert.equal(defaultRejectionReason(true), DEFAULT_DUPLICATE_REJECTION_REASON)
    assert.equal(defaultRejectionReason(false), '')
  })
})
