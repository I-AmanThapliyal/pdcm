import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ApiFailure, pdcm } from '../api'
import { useAuth } from '../auth'
import { formatWhen } from '../format'
import { suggestionStatusText } from '../suggestionStatus'
import type { DestinationSuggestion } from '../types'

export function MySuggestionsPage() {
  const { user, ready } = useAuth()
  const [items, setItems] = useState<DestinationSuggestion[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    setError(null)
    pdcm.myDestinationSuggestions()
      .then(setItems)
      .catch((reason: unknown) => {
        setError(reason instanceof ApiFailure ? reason.message : 'Could not load your suggestions.')
      })
  }, [user])

  if (!ready) return <p>Loading…</p>
  if (!user) return <Navigate to="/login" replace state={{ from: '/my-suggestions' }} />

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl">Your suggestions</h1>
        <p className="mt-2 max-w-2xl text-sm text-stone-600">
          Track status of places you proposed. Only administrators can approve or reject.
        </p>
        <p className="mt-2 text-sm">
          <Link to="/suggest" className="underline">Suggest another destination</Link>
        </p>
      </header>

      {error && <p className="text-clay">{error}</p>}

      {items.length === 0 ? (
        <p className="text-stone-700">You have not submitted any suggestions yet.</p>
      ) : (
        <ul className="divide-y divide-line rounded-2xl border border-line bg-white">
          {items.map((item) => {
            const catalogueMatchId =
              item.createdDestinationId ??
              (item.status === 'REJECTED' && item.possibleDuplicates.length > 0
                ? item.possibleDuplicates[0].destinationId
                : null)

            return (
              <li key={item.id} className="space-y-1 px-4 py-4 text-sm">
                <p className="font-medium">{item.name}</p>
                <p className="text-stone-600">
                  {item.city}, {item.region} · {item.category.replaceAll('_', ' ')} · submitted{' '}
                  {formatWhen(item.createdAt)}
                </p>
                <p>Status: {suggestionStatusText(item.status)}</p>
                {item.status === 'REJECTED' && (
                  <>
                    <p className="text-stone-600">Your suggestion was not added to the catalogue.</p>
                    {item.adminNote && <p>Reason: {item.adminNote}</p>}
                  </>
                )}
                {item.status === 'APPROVED' && item.adminNote && (
                  <p className="text-stone-600">Note: {item.adminNote}</p>
                )}
                {catalogueMatchId != null && (
                  <p>
                    <Link to={`/destinations/${catalogueMatchId}`} className="underline">
                      {item.status === 'APPROVED'
                        ? 'Open catalogue destination'
                        : 'View existing catalogue destination'}
                    </Link>
                  </p>
                )}
                {item.possibleDuplicate && item.status === 'SUBMITTED' && (
                  <p className="text-stone-600">Possible duplicates flagged for admin review.</p>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
