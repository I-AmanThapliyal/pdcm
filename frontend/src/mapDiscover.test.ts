import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
	destinationPlaceLine,
	discoverListMessage,
	discoverRefreshHint,
	resolveSelectedDestination,
	unknownIsNotQuiet,
} from './mapDiscover.ts'
import type { Destination } from './types.ts'

const sample: Destination = {
	id: 1,
	name: 'Hawa Mahal',
	slug: 'hawa-mahal',
	description: 'Palace of Winds',
	region: 'Rajasthan',
	city: 'Jaipur',
	category: 'HISTORICAL',
	latitude: 26.92,
	longitude: 75.82,
	crowdSensitivity: 'MEDIUM',
	tags: ['palace'],
}

const other: Destination = {
	...sample,
	id: 2,
	name: 'Amber Fort',
	slug: 'amber-fort',
}

describe('destinationPlaceLine', () => {
	it('prefers city when present', () => {
		assert.equal(destinationPlaceLine(sample), 'Jaipur · HISTORICAL')
	})

	it('falls back to region', () => {
		assert.equal(destinationPlaceLine({ ...sample, city: null }), 'Rajasthan · HISTORICAL')
	})
})

describe('discoverListMessage', () => {
	it('reports initial loading only when the list is empty', () => {
		assert.equal(
			discoverListMessage({ initialLoading: true, refreshing: false, error: null, count: 0 }),
			'Loading destinations in this view…',
		)
		assert.equal(
			discoverListMessage({ initialLoading: true, refreshing: false, error: null, count: 3 }),
			null,
		)
	})

	it('does not treat background refresh as a full loading state', () => {
		assert.equal(
			discoverListMessage({ initialLoading: false, refreshing: true, error: null, count: 3 }),
			null,
		)
	})

	it('reports error over empty', () => {
		assert.equal(
			discoverListMessage({ initialLoading: false, refreshing: false, error: 'boom', count: 0 }),
			'boom',
		)
	})

	it('reports empty when idle with zero results', () => {
		assert.match(
			discoverListMessage({ initialLoading: false, refreshing: false, error: null, count: 0 }) ?? '',
			/No destinations match/,
		)
	})

	it('returns null when results exist', () => {
		assert.equal(
			discoverListMessage({ initialLoading: false, refreshing: false, error: null, count: 3 }),
			null,
		)
	})
})

describe('discoverRefreshHint', () => {
	it('hints while refreshing existing rows', () => {
		assert.equal(
			discoverRefreshHint({ refreshing: true, error: null, count: 2 }),
			'Updating destinations for this view…',
		)
	})

	it('keeps a soft error when rows are preserved', () => {
		assert.equal(
			discoverRefreshHint({ refreshing: false, error: 'timeout', count: 2 }),
			'timeout',
		)
	})

	it('stays quiet when idle with rows', () => {
		assert.equal(discoverRefreshHint({ refreshing: false, error: null, count: 2 }), null)
	})
})

describe('resolveSelectedDestination', () => {
	it('prefers the destination from the latest list', () => {
		assert.equal(
			resolveSelectedDestination(sample, 2, [sample, other])?.slug,
			'amber-fort',
		)
	})

	it('keeps the previous selection when omitted from a refresh', () => {
		assert.equal(
			resolveSelectedDestination(sample, 1, [other])?.slug,
			'hawa-mahal',
		)
	})

	it('clears when nothing is selected', () => {
		assert.equal(resolveSelectedDestination(sample, null, [sample]), null)
	})
})

describe('unknownIsNotQuiet', () => {
	it('treats UNKNOWN as missing data', () => {
		assert.equal(unknownIsNotQuiet('UNKNOWN'), true)
		assert.equal(unknownIsNotQuiet('QUIET'), false)
		assert.equal(unknownIsNotQuiet('LIGHT'), false)
	})
})
