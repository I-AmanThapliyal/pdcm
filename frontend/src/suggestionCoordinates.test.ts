import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { parseSuggestionCoordinates } from './suggestionCoordinates.ts'

describe('parseSuggestionCoordinates', () => {
  it('accepts valid WGS84 coords inside soft India box', () => {
    const result = parseSuggestionCoordinates('28.541', '77.098')
    assert.equal(result.ok, true)
    if (result.ok) {
      assert.equal(result.latitude, 28.541)
      assert.equal(result.longitude, 77.098)
    }
  })

  it('rejects blank, non-numeric, out-of-range, null-island, and out-of-box', () => {
    assert.equal(parseSuggestionCoordinates('', '77').ok, false)
    assert.equal(parseSuggestionCoordinates('abc', '77').ok, false)
    assert.equal(parseSuggestionCoordinates('91', '77').ok, false)
    assert.equal(parseSuggestionCoordinates('28', '181').ok, false)
    assert.equal(parseSuggestionCoordinates('0', '0').ok, false)
    assert.equal(parseSuggestionCoordinates('1', '1').ok, false)
  })
})
