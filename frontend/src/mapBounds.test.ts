import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
	boundsNearlyEqual,
	buildMapQuery,
	clampBoundsToMaxSpan,
	defaultIndiaBounds,
} from './mapBounds.ts'

describe('clampBoundsToMaxSpan', () => {
	it('leaves India bounds unchanged', () => {
		const bounds = defaultIndiaBounds()
		assert.deepEqual(clampBoundsToMaxSpan(bounds), bounds)
	})

	it('shrinks oversized longitude span around the center', () => {
		const clamped = clampBoundsToMaxSpan({ south: 10, west: 60, north: 20, east: 120 }, 40)
		assert.equal(clamped.east - clamped.west, 40)
		assert.equal((clamped.west + clamped.east) / 2, 90)
	})
})

describe('boundsNearlyEqual', () => {
	it('treats tiny leaflet jitter as unchanged', () => {
		const base = defaultIndiaBounds()
		assert.equal(
			boundsNearlyEqual(base, {
				...base,
				south: base.south + 0.0001,
				east: base.east - 0.0001,
			}),
			true,
		)
	})

	it('detects a real pan', () => {
		const base = defaultIndiaBounds()
		assert.equal(
			boundsNearlyEqual(base, {
				...base,
				south: base.south + 0.5,
				north: base.north + 0.5,
			}),
			false,
		)
	})
})

describe('buildMapQuery', () => {
	it('includes filters and clamped bounds', () => {
		const query = buildMapQuery({
			bounds: { south: 6.5, west: 68, north: 35.5, east: 97 },
			q: 'fort',
			region: 'Rajasthan',
			category: 'HISTORICAL',
			limit: 50,
		})
		assert.equal(query.get('q'), 'fort')
		assert.equal(query.get('region'), 'Rajasthan')
		assert.equal(query.get('category'), 'HISTORICAL')
		assert.equal(query.get('limit'), '50')
		assert.equal(query.get('south'), '6.5')
	})
})
