import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiFailure, pdcm } from '../api'
import { CrowdBadge, ConfidenceNote } from '../components/Badges'
import { DestinationMap } from '../components/DestinationMap'
import { ExternalTrafficPanel } from '../components/ExternalTrafficPanel'
import {
  boundsNearlyEqual,
  buildMapQuery,
  defaultIndiaBounds,
  type MapBounds,
} from '../mapBounds'
import {
  destinationPlaceLine,
  discoverListMessage,
  discoverRefreshHint,
  resolveSelectedDestination,
  type DiscoverView,
} from '../mapDiscover'
import type { Category, CrowdSummary, Destination, ExternalTrafficSummary, TrafficSummary } from '../types'

const CATEGORIES: Category[] = ['HISTORICAL', 'TEMPLE', 'BEACH', 'HILL_STATION', 'WILDLIFE', 'CITY', 'NATURE']

export function MapDiscoverPage() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const region = params.get('region') ?? ''
  const category = params.get('category') ?? ''
  const view = (params.get('view') === 'list' ? 'list' : 'map') as DiscoverView

  const [bounds, setBounds] = useState<MapBounds>(defaultIndiaBounds)
  const [destinations, setDestinations] = useState<Destination[]>([])
  const [initialLoading, setInitialLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [selectedDestination, setSelectedDestination] = useState<Destination | null>(null)
  const [crowd, setCrowd] = useState<CrowdSummary | null>(null)
  const [traffic, setTraffic] = useState<TrafficSummary | null>(null)
  const [externalTraffic, setExternalTraffic] = useState<ExternalTrafficSummary | null>(null)
  const [detailError, setDetailError] = useState<string | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const hasLoadedOnce = useRef(false)
  const lastFetchedQuery = useRef<string | null>(null)

  const onBoundsChange = useCallback((next: MapBounds) => {
    setBounds((current) => (boundsNearlyEqual(current, next) ? current : next))
  }, [])

  useEffect(() => {
    const query = buildMapQuery({ bounds, q, region, category })
    const queryKey = query.toString()
    if (lastFetchedQuery.current === queryKey) {
      return
    }

    let cancelled = false
    if (hasLoadedOnce.current) {
      setRefreshing(true)
    } else {
      setInitialLoading(true)
    }

    const handle = window.setTimeout(() => {
      pdcm.destinationsMap(query)
        .then((items) => {
          if (cancelled) return
          lastFetchedQuery.current = queryKey
          hasLoadedOnce.current = true
          setDestinations(items)
          setError(null)
        })
        .catch((reason: unknown) => {
          if (cancelled) return
          const message = reason instanceof ApiFailure ? reason.message : 'Map destinations could not be loaded.'
          setError(message)
          if (!hasLoadedOnce.current) {
            setDestinations([])
          }
        })
        .finally(() => {
          if (cancelled) return
          setInitialLoading(false)
          setRefreshing(false)
        })
    }, 250)

    return () => {
      cancelled = true
      window.clearTimeout(handle)
    }
  }, [bounds, q, region, category])

  useEffect(() => {
    setSelectedDestination((previous) =>
      resolveSelectedDestination(previous, selectedId, destinations),
    )
  }, [destinations, selectedId])

  useEffect(() => {
    if (!selectedId) {
      setCrowd(null)
      setTraffic(null)
      setExternalTraffic(null)
      setDetailError(null)
      setDetailLoading(false)
      return
    }
    let cancelled = false
    setDetailLoading(true)
    setDetailError(null)
    setExternalTraffic(null)
    Promise.all([pdcm.crowd(String(selectedId)), pdcm.traffic(String(selectedId))])
      .then(([crowdSummary, trafficSummary]) => {
        if (cancelled) return
        setCrowd(crowdSummary)
        setTraffic(trafficSummary)
      })
      .catch((reason: unknown) => {
        if (cancelled) return
        setCrowd(null)
        setTraffic(null)
        setDetailError(reason instanceof ApiFailure ? reason.message : 'Crowd and traffic could not be loaded.')
      })
      .finally(() => {
        if (!cancelled) setDetailLoading(false)
      })
    pdcm.trafficExternal(String(selectedId))
      .then((summary) => {
        if (!cancelled) setExternalTraffic(summary)
      })
      .catch(() => {
        if (!cancelled) setExternalTraffic(null)
      })
    return () => {
      cancelled = true
    }
  }, [selectedId])

  function update(next: Record<string, string>) {
    const merged = new URLSearchParams(params)
    for (const [key, value] of Object.entries(next)) {
      if (value) merged.set(key, value)
      else merged.delete(key)
    }
    // Filter changes must refetch even if a previous identical query was cached after error recovery.
    if ('q' in next || 'region' in next || 'category' in next) {
      lastFetchedQuery.current = null
    }
    setParams(merged)
  }

  function selectDestination(destination: Destination) {
    setSelectedId(destination.id)
    setSelectedDestination(destination)
  }

  const statusMessage = discoverListMessage({
    initialLoading,
    refreshing,
    error,
    count: destinations.length,
  })
  const refreshHint = discoverRefreshHint({
    refreshing,
    error,
    count: destinations.length,
  })

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-4xl">Discover on the map</h1>
          <p className="mt-2 max-w-2xl text-stone-700">
            Browse verified Indian destinations. Crowd and traffic load only for the place you select — missing reports stay Unknown, not quiet.
          </p>
        </div>
        <div className="flex gap-2" role="group" aria-label="View mode">
          <button
            type="button"
            className={`rounded-full px-3 py-1.5 text-sm ${view === 'map' ? 'bg-ink text-paper' : 'border border-line bg-white'}`}
            aria-pressed={view === 'map'}
            onClick={() => update({ view: 'map' })}
          >
            Map
          </button>
          <button
            type="button"
            className={`rounded-full px-3 py-1.5 text-sm ${view === 'list' ? 'bg-ink text-paper' : 'border border-line bg-white'}`}
            aria-pressed={view === 'list'}
            onClick={() => update({ view: 'list' })}
          >
            List
          </button>
        </div>
      </header>

      <form className="grid gap-3 sm:grid-cols-3" onSubmit={(event) => event.preventDefault()}>
        <input
          aria-label="Search destinations"
          className="rounded-xl border border-line bg-white px-3 py-2"
          placeholder="Search name or city"
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
          {CATEGORIES.map((item) => (
            <option key={item} value={item}>{item.replaceAll('_', ' ')}</option>
          ))}
        </select>
      </form>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.6fr)_minmax(16rem,0.9fr)]">
        <div className="space-y-4">
          {view === 'map' ? (
            <DestinationMap
              destinations={destinations}
              selected={selectedDestination}
              onSelect={selectDestination}
              onBoundsChange={onBoundsChange}
            />
          ) : null}

          <section aria-labelledby="map-list-heading">
            <h2 id="map-list-heading" className="text-2xl">
              {view === 'list' ? 'Destinations' : 'Accessible list'}
            </h2>
            {statusMessage && (
              <p className={`mt-2 ${error && destinations.length === 0 ? 'text-clay' : 'text-stone-600'}`} role="status">
                {statusMessage}
              </p>
            )}
            {refreshHint && (
              <p className={`mt-2 text-sm ${error && destinations.length > 0 ? 'text-clay' : 'text-stone-600'}`} role="status">
                {refreshHint}
              </p>
            )}
            {!statusMessage && !refreshHint && (
              <p className="mt-2 text-sm text-stone-600" role="status">
                Showing {destinations.length} destination{destinations.length === 1 ? '' : 's'} in view.
              </p>
            )}
            <ul className="mt-3 max-h-[28rem] divide-y divide-line overflow-y-auto rounded-2xl border border-line bg-white">
              {destinations.map((destination) => {
                const selectedItem = destination.id === selectedId
                return (
                  <li key={destination.id}>
                    <button
                      type="button"
                      className={`block w-full px-4 py-3 text-left hover:bg-paper focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-ink ${selectedItem ? 'bg-paper' : ''}`}
                      aria-current={selectedItem ? 'true' : undefined}
                      onClick={() => selectDestination(destination)}
                    >
                      <span className="text-lg">{destination.name}</span>
                      <span className="mt-1 block text-sm text-stone-600">{destinationPlaceLine(destination)}</span>
                    </button>
                  </li>
                )
              })}
            </ul>
          </section>
        </div>

        <aside className="rounded-2xl border border-line bg-white p-5" aria-live="polite">
          {!selectedDestination && (
            <p className="text-stone-600">Select a destination from the map or list to see crowd and traffic.</p>
          )}
          {selectedDestination && (
            <div className="space-y-4">
              <div>
                <h2 className="text-2xl">{selectedDestination.name}</h2>
                <p className="mt-1 text-sm text-stone-600">{destinationPlaceLine(selectedDestination)}</p>
              </div>
              {detailLoading && <p>Loading crowd and traffic…</p>}
              {detailError && <p className="text-clay">{detailError}</p>}
              {crowd && traffic && !detailLoading && (
                <>
                  <div>
                    <h3 className="text-sm font-medium text-stone-600">Crowd now</h3>
                    <div className="mt-2 flex flex-wrap items-center gap-2">
                      <CrowdBadge status={crowd.status} />
                      <ConfidenceNote confidence={crowd.confidence} />
                    </div>
                    {crowd.status === 'UNKNOWN' && (
                      <p className="mt-2 text-sm text-stone-600">No usable recent reports. Unknown is not quiet.</p>
                    )}
                  </div>
                  <ExternalTrafficPanel community={traffic} external={externalTraffic} />
                </>
              )}
              <Link to={`/destinations/${selectedDestination.id}`} className="inline-block underline">
                Open destination detail
              </Link>
            </div>
          )}
        </aside>
      </div>

      <p className="text-xs text-stone-600">
        Map data ©{' '}
        <a className="underline" href="https://www.openstreetmap.org/copyright">
          OpenStreetMap contributors
        </a>
        . Community tiles are best-effort with no availability guarantee. Tile URL is configurable via{' '}
        <code>VITE_OSM_TILE_URL</code>.
      </p>
    </div>
  )
}
