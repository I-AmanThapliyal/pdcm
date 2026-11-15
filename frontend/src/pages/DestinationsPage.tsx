import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiFailure, pdcm } from '../api'
import type { Category, Destination, Page } from '../types'

const CATEGORIES: Category[] = ['HISTORICAL', 'TEMPLE', 'BEACH', 'HILL_STATION', 'WILDLIFE', 'CITY', 'NATURE']

export function DestinationsPage() {
  const [params, setParams] = useSearchParams()
  const [page, setPage] = useState<Page<Destination> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const q = params.get('q') ?? ''
  const region = params.get('region') ?? ''
  const category = params.get('category') ?? ''
  const pageNumber = Number(params.get('page') ?? '0')

  useEffect(() => {
    const query = new URLSearchParams()
    if (q) query.set('q', q)
    if (region) query.set('region', region)
    if (category) query.set('category', category)
    query.set('page', String(pageNumber))
    query.set('size', '20')
    setError(null)
    pdcm.destinations(query)
      .then(setPage)
      .catch((reason: unknown) => setError(reason instanceof ApiFailure ? reason.message : 'Destinations could not be loaded.'))
  }, [q, region, category, pageNumber])

  function update(next: Record<string, string>) {
    const merged = new URLSearchParams(params)
    for (const [key, value] of Object.entries(next)) {
      if (value) merged.set(key, value)
      else merged.delete(key)
    }
    if (!('page' in next)) merged.delete('page')
    setParams(merged)
  }

  return (
    <div>
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="text-4xl">Destinations</h1>
        <Link to="/map" className="underline">Open map discover</Link>
      </div>
      <form className="mt-4 grid gap-3 sm:grid-cols-3" onSubmit={(event) => event.preventDefault()}>
        <input
          aria-label="Search"
          className="rounded-xl border border-line bg-white px-3 py-2"
          placeholder="Search name"
          value={q}
          onChange={(event) => update({ q: event.target.value })}
        />
        <input
          aria-label="Region"
          className="rounded-xl border border-line bg-white px-3 py-2"
          placeholder="Region"
          value={region}
          onChange={(event) => update({ region: event.target.value })}
        />
        <select
          aria-label="Category"
          className="rounded-xl border border-line bg-white px-3 py-2"
          value={category}
          onChange={(event) => update({ category: event.target.value })}
        >
          <option value="">All categories</option>
          {CATEGORIES.map((item) => <option key={item} value={item}>{item.replace('_', ' ')}</option>)}
        </select>
      </form>
      {error && <p className="mt-4 text-clay">{error}</p>}
      <ul className="mt-6 divide-y divide-line rounded-2xl border border-line bg-white">
        {page?.content.map((destination) => (
          <li key={destination.id}>
            <Link to={`/destinations/${destination.id}`} className="block px-4 py-4 hover:bg-paper">
              <span className="text-lg">{destination.name}</span>
              <span className="mt-1 block text-sm text-stone-600">{destination.region} · {destination.category.replace('_', ' ')}</span>
            </Link>
          </li>
        ))}
      </ul>
      {page && page.totalPages > 1 && (
        <div className="mt-4 flex gap-3">
          <button type="button" disabled={page.first} className="underline disabled:text-stone-400" onClick={() => update({ page: String(page.page - 1) })}>Previous</button>
          <button type="button" disabled={page.last} className="underline disabled:text-stone-400" onClick={() => update({ page: String(page.page + 1) })}>Next</button>
        </div>
      )}
    </div>
  )
}
