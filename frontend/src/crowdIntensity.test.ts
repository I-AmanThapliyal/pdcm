import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { parseCrowdIntensity } from './crowdIntensity.ts'

describe('parseCrowdIntensity', () => {
	for (const value of ['1', '2', '3', '4', '5']) {
		it(`accepts ${value}`, () => {
			assert.equal(parseCrowdIntensity(value), Number(value))
		})
	}

	for (const value of ['0', '-1', '6', '9', '09', '1.5', '3.0', '', '  ', 'abc', '1e1']) {
		it(`rejects ${JSON.stringify(value)}`, () => {
			assert.equal(parseCrowdIntensity(value), null)
		})
	}
})
