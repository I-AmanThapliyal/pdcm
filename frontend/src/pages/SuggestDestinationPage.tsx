import { useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ApiFailure, pdcm } from '../api'
import { useAuth } from '../auth'
import { duplicateReasonText } from '../suggestionStatus'
import type { Category, DestinationSuggestion } from '../types'

const CATEGORIES: Category[] = [
  'HISTORICAL',
  'TEMPLE',
  'BEACH',
  'HILL_STATION',
  'WILDLIFE',
  'CITY',
  'NATURE',
]

export function SuggestDestinationPage() {
  const { user, ready } = useAuth()
  const [name, setName] = useState('')
  const [city, setCity] = useState('')
  const [region, setRegion] = useState('')
  const [category, setCategory] = useState<Category>('NATURE')
  const [description, setDescription] = useState('')
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [referenceUrl, setReferenceUrl] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState<DestinationSuggestion | null>(null)
  const [busy, setBusy] = useState(false)

  if (!ready) return <p>Loading…</p>
  if (!user) return <Navigate to="/login" replace state={{ from: '/suggest' }} />

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitted(null)
    setBusy(true)
    try {
      const lat = latitude.trim() === '' ? null : Number(latitude)
      const lon = longitude.trim() === '' ? null : Number(longitude)
      if ((lat == null) !== (lon == null)) {
        setError('Provide both latitude and longitude, or leave both blank.')
        return
      }
      if (lat != null && (Number.isNaN(lat) || Number.isNaN(lon!))) {
        setError('Latitude and longitude must be numbers.')
        return
      }
      const result = await pdcm.submitDestinationSuggestion({
        name: name.trim(),
        city: city.trim(),
        region: region.trim(),
        category,
        description: description.trim(),
        latitude: lat,
        longitude: lon,
        referenceUrl: referenceUrl.trim() === '' ? null : referenceUrl.trim(),
      })
      setSubmitted(result)
      setName('')
      setCity('')
      setRegion('')
      setDescription('')
      setLatitude('')
      setLongitude('')
      setReferenceUrl('')
    } catch (reason) {
      const details = reason instanceof ApiFailure ? reason.details.filter(Boolean).join(' ') : ''
      setError(reason instanceof ApiFailure ? `${reason.message}${details ? ` ${details}` : ''}` : 'Could not submit suggestion.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl">Suggest a destination</h1>
        <p className="mt-2 max-w-2xl text-sm text-stone-600">
          Propose a place for the catalogue. Suggestions are reviewed by an administrator before anything
          is published. Crowd and traffic stay unknown until real visit reports exist — we never invent them.
        </p>
        <p className="mt-2 text-sm">
          <Link to="/my-suggestions" className="underline">View your suggestions</Link>
        </p>
      </header>

      <form className="max-w-xl space-y-4 rounded-3xl border border-line bg-white p-6" onSubmit={onSubmit}>
        <label className="block text-sm">Destination name
          <input className="mt-1 w-full rounded-lg border border-line px-3 py-2" value={name} onChange={(e) => setName(e.target.value)} required maxLength={200} />
        </label>
        <label className="block text-sm">City / nearest town
          <input className="mt-1 w-full rounded-lg border border-line px-3 py-2" value={city} onChange={(e) => setCity(e.target.value)} required maxLength={120} />
        </label>
        <label className="block text-sm">State / UT
          <input className="mt-1 w-full rounded-lg border border-line px-3 py-2" value={region} onChange={(e) => setRegion(e.target.value)} required maxLength={100} />
        </label>
        <label className="block text-sm">Category
          <select className="mt-1 w-full rounded-lg border border-line px-3 py-2" value={category} onChange={(e) => setCategory(e.target.value as Category)}>
            {CATEGORIES.map((value) => (
              <option key={value} value={value}>{value.replaceAll('_', ' ')}</option>
            ))}
          </select>
        </label>
        <label className="block text-sm">Description / reason
          <textarea className="mt-1 w-full rounded-lg border border-line px-3 py-2" rows={4} value={description} onChange={(e) => setDescription(e.target.value)} required maxLength={4000} />
        </label>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block text-sm">Latitude (optional)
            <input className="mt-1 w-full rounded-lg border border-line px-3 py-2" value={latitude} onChange={(e) => setLatitude(e.target.value)} inputMode="decimal" placeholder="e.g. 18.52" />
          </label>
          <label className="block text-sm">Longitude (optional)
            <input className="mt-1 w-full rounded-lg border border-line px-3 py-2" value={longitude} onChange={(e) => setLongitude(e.target.value)} inputMode="decimal" placeholder="e.g. 73.85" />
          </label>
        </div>
        <label className="block text-sm">Reference URL (optional)
          <input className="mt-1 w-full rounded-lg border border-line px-3 py-2" type="url" value={referenceUrl} onChange={(e) => setReferenceUrl(e.target.value)} maxLength={500} placeholder="https://" />
        </label>
        {error && <p className="text-sm text-clay">{error}</p>}
        <button type="submit" className="rounded-full bg-ink px-4 py-2 text-paper" disabled={busy}>
          {busy ? 'Submitting…' : 'Submit suggestion'}
        </button>
      </form>

      {submitted && (
        <div className="max-w-xl space-y-2 rounded-3xl border border-line bg-white p-6 text-sm">
          <p className="font-medium">Suggestion #{submitted.id} submitted.</p>
          {submitted.possibleDuplicate ? (
            <div className="space-y-1 text-stone-700">
              <p>Possible catalogue matches were found (still submitted for admin review):</p>
              <ul className="list-disc space-y-1 pl-5">
                {submitted.possibleDuplicates.map((dup) => (
                  <li key={dup.destinationId}>
                    <Link to={`/destinations/${dup.destinationId}`} className="underline">{dup.name}</Link>
                    {' · '}
                    {duplicateReasonText(dup.matchReason)}
                    {dup.distanceKm != null ? ` · ${dup.distanceKm.toFixed(1)} km` : ''}
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <p className="text-stone-700">No obvious catalogue duplicates detected.</p>
          )}
        </div>
      )}
    </div>
  )
}
