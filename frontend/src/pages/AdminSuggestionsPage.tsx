import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ApiFailure, pdcm } from '../api'
import { useAuth } from '../auth'
import { formatWhen } from '../format'
import { parseSuggestionCoordinates } from '../suggestionCoordinates'
import {
  REJECTION_REASON_MAX_LENGTH,
  defaultRejectionReason,
  parseRejectionReason,
} from '../suggestionRejection'
import { duplicateReasonText, suggestionStatusText } from '../suggestionStatus'
import type { DestinationSuggestion, SuggestionStatus } from '../types'

export function AdminSuggestionsPage() {
  const { user, ready } = useAuth()
  const [items, setItems] = useState<DestinationSuggestion[]>([])
  const [queue, setQueue] = useState<Extract<SuggestionStatus, 'SUBMITTED' | 'REJECTED'>>('SUBMITTED')
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [selected, setSelected] = useState<DestinationSuggestion | null>(null)
  const [latInput, setLatInput] = useState('')
  const [lonInput, setLonInput] = useState('')
  const [coordsBusy, setCoordsBusy] = useState(false)
  const [rejectTarget, setRejectTarget] = useState<DestinationSuggestion | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const [rejectError, setRejectError] = useState<string | null>(null)

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') return
    setError(null)
    pdcm.adminDestinationSuggestions(queue)
      .then(setItems)
      .catch((reason: unknown) => {
        setError(reason instanceof ApiFailure ? reason.message : 'Could not load suggestion queue.')
      })
  }, [user, queue, message])

  if (!ready) return <p>Loading…</p>
  if (!user) return <Navigate to="/login" replace />
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />

  function syncCoordInputs(detail: DestinationSuggestion) {
    setLatInput(detail.latitude == null ? '' : String(detail.latitude))
    setLonInput(detail.longitude == null ? '' : String(detail.longitude))
  }

  async function loadDetail(id: number) {
    setError(null)
    try {
      const detail = await pdcm.adminDestinationSuggestion(id)
      setSelected(detail)
      syncCoordInputs(detail)
    } catch (reason) {
      setError(reason instanceof ApiFailure ? reason.message : 'Could not load suggestion detail.')
    }
  }

  async function saveCoordinates() {
    if (!selected || selected.status !== 'SUBMITTED') return
    const parsed = parseSuggestionCoordinates(latInput, lonInput)
    if (!parsed.ok) {
      setError(parsed.message)
      return
    }
    setCoordsBusy(true)
    setError(null)
    setMessage(null)
    try {
      const updated = await pdcm.adminUpdateSuggestionCoordinates(
        selected.id,
        parsed.latitude,
        parsed.longitude,
      )
      setSelected(updated)
      syncCoordInputs(updated)
      setMessage(`Coordinates saved for suggestion ${selected.id}.`)
      setItems((prev) => prev.map((item) => (item.id === updated.id ? { ...item, ...updated } : item)))
    } catch (reason) {
      setError(reason instanceof ApiFailure ? reason.message : 'Could not save coordinates.')
    } finally {
      setCoordsBusy(false)
    }
  }

  async function approve(id: number) {
    setBusyId(id)
    setMessage(null)
    setError(null)
    try {
      await pdcm.adminApproveDestinationSuggestion(id, 'Approved after review')
      setMessage(`Suggestion ${id} approved into the catalogue.`)
      setSelected(null)
    } catch (reason) {
      setError(reason instanceof ApiFailure ? reason.message : 'Suggestion action failed.')
    } finally {
      setBusyId(null)
    }
  }

  function openRejectDialog(item: DestinationSuggestion) {
    setRejectTarget(item)
    setRejectReason(defaultRejectionReason(item.possibleDuplicate))
    setRejectError(null)
  }

  function closeRejectDialog() {
    setRejectTarget(null)
    setRejectReason('')
    setRejectError(null)
  }

  async function confirmReject() {
    if (!rejectTarget) return
    const parsed = parseRejectionReason(rejectReason)
    if (!parsed.ok) {
      setRejectError(parsed.message)
      return
    }
    setBusyId(rejectTarget.id)
    setMessage(null)
    setError(null)
    setRejectError(null)
    try {
      const updated = await pdcm.adminRejectDestinationSuggestion(rejectTarget.id, parsed.reason)
      setMessage(
        `Suggestion ${updated.id} rejected. Reason: ${updated.adminNote ?? parsed.reason}`,
      )
      setSelected(updated)
      syncCoordInputs(updated)
      closeRejectDialog()
    } catch (reason) {
      setRejectError(reason instanceof ApiFailure ? reason.message : 'Suggestion rejection failed.')
    } finally {
      setBusyId(null)
    }
  }

  const canEditCoords = selected?.status === 'SUBMITTED'
  const hasCoords = selected?.latitude != null && selected?.longitude != null

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl">Destination suggestions</h1>
        <p className="mt-2 max-w-2xl text-sm text-stone-600">
          ADMIN only. Approve creates a catalogue destination with UNKNOWN crowd/traffic and no synthetic reports.
          Contributor email is never shown — only contributor user id.
        </p>
      </header>

      <div className="flex gap-3 text-sm">
        <button
          type="button"
          className={queue === 'SUBMITTED' ? 'underline font-medium' : 'text-stone-600'}
          onClick={() => setQueue('SUBMITTED')}
        >
          Submitted
        </button>
        <button
          type="button"
          className={queue === 'REJECTED' ? 'underline font-medium' : 'text-stone-600'}
          onClick={() => setQueue('REJECTED')}
        >
          Rejected
        </button>
      </div>

      {error && <p className="text-clay">{error}</p>}
      {message && <p className="text-stone-700">{message}</p>}

      {items.length === 0 ? (
        <p className="text-stone-700">No suggestions in this queue.</p>
      ) : (
        <ul className="divide-y divide-line rounded-2xl border border-line bg-white">
          {items.map((item) => (
            <li key={item.id} className="space-y-2 px-4 py-4 text-sm">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <p className="font-medium">{item.name}</p>
                  <p className="text-stone-600">
                    #{item.id} · {item.city}, {item.region} · {item.category.replaceAll('_', ' ')} · contributor #
                    {item.contributorUserId} · {formatWhen(item.createdAt)}
                  </p>
                  <p className="text-stone-600">{suggestionStatusText(item.status)}</p>
                  {item.status === 'REJECTED' && item.adminNote && (
                    <p className="text-stone-600">Reason: {item.adminNote}</p>
                  )}
                  {item.possibleDuplicate && (
                    <p className="text-stone-600">Possible duplicates: {item.possibleDuplicates.length}</p>
                  )}
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    className="rounded-lg border border-line px-3 py-1.5"
                    onClick={() => loadDetail(item.id)}
                  >
                    Details
                  </button>
                  {queue === 'SUBMITTED' && (
                    <>
                      <button
                        type="button"
                        className="rounded-lg border border-line px-3 py-1.5"
                        disabled={busyId === item.id}
                        onClick={() => void approve(item.id)}
                      >
                        Approve
                      </button>
                      <button
                        type="button"
                        className="rounded-lg border border-line px-3 py-1.5"
                        disabled={busyId === item.id}
                        onClick={() => openRejectDialog(item)}
                      >
                        Reject
                      </button>
                    </>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}

      {rejectTarget && (
        <div className="space-y-3 rounded-2xl border border-line bg-white p-5 text-sm">
          <h2 className="text-xl">Reject suggestion #{rejectTarget.id}</h2>
          <p className="text-stone-600">
            {rejectTarget.name} · {rejectTarget.city}, {rejectTarget.region}
          </p>
          <p className="text-stone-600">
            A rejection reason is required and will be shown to the contributor. Catalogue destinations are not
            changed.
          </p>
          <label className="block">
            <span className="text-stone-600">Reason (required, max {REJECTION_REASON_MAX_LENGTH})</span>
            <textarea
              className="mt-1 w-full rounded-lg border border-line px-3 py-2"
              rows={4}
              maxLength={REJECTION_REASON_MAX_LENGTH}
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              required
            />
          </label>
          <p className="text-stone-600">{rejectReason.trim().length} / {REJECTION_REASON_MAX_LENGTH}</p>
          {rejectError && <p className="text-clay">{rejectError}</p>}
          <div className="flex gap-2">
            <button
              type="button"
              className="rounded-lg border border-line px-3 py-1.5"
              disabled={busyId === rejectTarget.id}
              onClick={() => void confirmReject()}
            >
              Confirm reject
            </button>
            <button type="button" className="underline" onClick={closeRejectDialog}>
              Cancel
            </button>
          </div>
        </div>
      )}

      {selected && (
        <div className="space-y-3 rounded-2xl border border-line bg-white p-5 text-sm">
          <div className="flex items-start justify-between gap-3">
            <h2 className="text-xl">{selected.name}</h2>
            <button type="button" className="underline" onClick={() => setSelected(null)}>Close</button>
          </div>
          <p className="text-stone-600">
            {selected.city}, {selected.region} · {selected.category.replaceAll('_', ' ')} · contributor #
            {selected.contributorUserId}
          </p>
          <p>{selected.description}</p>
          <p className="text-stone-600">Status: {suggestionStatusText(selected.status)}</p>
          {selected.status === 'REJECTED' && selected.adminNote && (
            <p>Rejection reason: {selected.adminNote}</p>
          )}
          <div className="space-y-2">
            <p className="text-stone-600">
              Coords:{' '}
              {!hasCoords
                ? 'not provided (cannot approve until present)'
                : `${selected.latitude}, ${selected.longitude}`}
            </p>
            {canEditCoords && (
              <div className="flex flex-wrap items-end gap-3">
                <label className="block">
                  <span className="text-stone-600">Latitude</span>
                  <input
                    type="text"
                    inputMode="decimal"
                    className="mt-1 block w-40 rounded-lg border border-line px-3 py-1.5"
                    value={latInput}
                    onChange={(e) => setLatInput(e.target.value)}
                    placeholder="-90 to 90"
                  />
                </label>
                <label className="block">
                  <span className="text-stone-600">Longitude</span>
                  <input
                    type="text"
                    inputMode="decimal"
                    className="mt-1 block w-40 rounded-lg border border-line px-3 py-1.5"
                    value={lonInput}
                    onChange={(e) => setLonInput(e.target.value)}
                    placeholder="-180 to 180"
                  />
                </label>
                <button
                  type="button"
                  className="rounded-lg border border-line px-3 py-1.5"
                  disabled={coordsBusy}
                  onClick={() => void saveCoordinates()}
                >
                  {coordsBusy ? 'Saving…' : 'Save coordinates'}
                </button>
              </div>
            )}
          </div>
          {selected.referenceUrl && (
            <p>
              Reference:{' '}
              <a href={selected.referenceUrl} className="underline" target="_blank" rel="noreferrer">
                {selected.referenceUrl}
              </a>
            </p>
          )}
          {selected.possibleDuplicates.length > 0 && (
            <div>
              <p className="font-medium">Possible duplicates</p>
              <ul className="mt-1 list-disc space-y-1 pl-5">
                {selected.possibleDuplicates.map((dup) => (
                  <li key={dup.destinationId}>
                    <Link to={`/destinations/${dup.destinationId}`} className="underline">{dup.name}</Link>
                    {' · '}
                    {duplicateReasonText(dup.matchReason)}
                    {dup.distanceKm != null ? ` · ${dup.distanceKm.toFixed(1)} km` : ''}
                  </li>
                ))}
              </ul>
            </div>
          )}
          {selected.auditHistory && selected.auditHistory.length > 0 && (
            <div>
              <p className="font-medium">Audit trail</p>
              <ul className="mt-1 space-y-1">
                {selected.auditHistory.map((event) => (
                  <li key={event.id} className="text-stone-600">
                    {event.action} by #{event.actorUserId} · {formatWhen(event.createdAt)}
                    {event.note ? ` · ${event.note}` : ''}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
