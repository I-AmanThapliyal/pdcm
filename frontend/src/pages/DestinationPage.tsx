import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiFailure, pdcm } from '../api'
import { useAuth } from '../auth'
import { ConfidenceNote, CrowdBadge } from '../components/Badges'
import { ExternalTrafficPanel } from '../components/ExternalTrafficPanel'
import { CROWD_INTENSITY_ERROR, parseCrowdIntensity } from '../crowdIntensity'
import { calendarFacts, formatWhen, indiaDate, localDateTimeValue } from '../format'
import { captureClientLocation, verificationMessage } from '../reportLocation'
import type { Alternative, CrowdSummary, Destination, ExternalTrafficSummary, Forecast, Nearby, TrafficSummary } from '../types'

export function DestinationPage() {
  const { id = '' } = useParams()
  const { user } = useAuth()
  const [destination, setDestination] = useState<Destination | null>(null)
  const [crowd, setCrowd] = useState<CrowdSummary | null>(null)
  const [traffic, setTraffic] = useState<TrafficSummary | null>(null)
  const [externalTraffic, setExternalTraffic] = useState<ExternalTrafficSummary | null>(null)
  const [forecastDate, setForecastDate] = useState(indiaDate(1))
  const [forecast, setForecast] = useState<Forecast | null>(null)
  const [alternatives, setAlternatives] = useState<Alternative[]>([])
  const [nearby, setNearby] = useState<Nearby[]>([])
  const [error, setError] = useState<string | null>(null)
  const [reportMessage, setReportMessage] = useState<string | null>(null)
  const [intensity, setIntensity] = useState('3')
  const [trafficCondition, setTrafficCondition] = useState('MODERATE')
  const [waitMinutes, setWaitMinutes] = useState('')
  const [note, setNote] = useState('')
  const [observedAt, setObservedAt] = useState(localDateTimeValue())
  const [shareLocation, setShareLocation] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    setError(null)
    setExternalTraffic(null)
    Promise.all([
      pdcm.destination(id),
      pdcm.crowd(id),
      pdcm.traffic(id),
      pdcm.alternatives(id),
      pdcm.nearby(id),
    ]).then(([place, crowdSummary, trafficSummary, alternativeList, nearbyList]) => {
      setDestination(place)
      setCrowd(crowdSummary)
      setTraffic(trafficSummary)
      setAlternatives(alternativeList)
      setNearby(nearbyList)
    }).catch((reason: unknown) => {
      setError(reason instanceof ApiFailure ? reason.message : 'This destination could not be loaded.')
    })
    pdcm.trafficExternal(id)
      .then(setExternalTraffic)
      .catch(() => setExternalTraffic(null))
  }, [id, reportMessage])

  useEffect(() => {
    pdcm.forecast(id, forecastDate).then(setForecast).catch(() => setForecast(null))
  }, [id, forecastDate])

  async function submitReport(event: FormEvent) {
    event.preventDefault()
    setReportMessage(null)
    const crowdIntensity = parseCrowdIntensity(intensity)
    if (crowdIntensity === null) {
      setReportMessage(CROWD_INTENSITY_ERROR)
      return
    }
    setSubmitting(true)
    try {
      const locationCapture = await captureClientLocation(shareLocation)
      const saved = await pdcm.report(id, {
        crowdIntensity,
        trafficCondition,
        waitMinutes: waitMinutes === '' ? null : Number(waitMinutes),
        note: note.trim() === '' ? null : note.trim(),
        observedAt: new Date(observedAt).toISOString(),
        locationConsent: shareLocation,
        clientLocation: locationCapture.status === 'captured' ? locationCapture.location : null,
      })
      setReportMessage(
        verificationMessage(saved.verificationStatus, saved.verificationReason, locationCapture),
      )
    } catch (reason) {
      setReportMessage(reason instanceof ApiFailure ? reason.message : 'The report could not be saved.')
    } finally {
      setSubmitting(false)
    }
  }

  if (error) return <p className="text-clay">{error}</p>
  if (!destination || !crowd || !traffic) return <p>Loading destination…</p>

  return (
    <div className="space-y-8">
      <header>
        <p className="text-sm text-stone-600">{destination.region} · {destination.category.replace('_', ' ')}</p>
        <h1 className="text-4xl">{destination.name}</h1>
        <p className="mt-3 max-w-3xl">{destination.description}</p>
        <p className="mt-2 text-sm text-stone-600">Tags: {destination.tags.join(', ') || 'none'}</p>
      </header>

      <section className="grid gap-4 lg:grid-cols-2">
        <article className="rounded-2xl border border-line bg-white p-5">
          <h2 className="text-2xl">Crowd now</h2>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <CrowdBadge status={crowd.status} />
            <ConfidenceNote confidence={crowd.confidence} />
          </div>
          <p className="mt-3 text-sm">
            {crowd.status === 'UNKNOWN'
              ? 'No location-verified reports in the last 6 hours. Unknown is not quiet.'
              : `Score ${crowd.score} from ${crowd.contributingReports} location-verified report${crowd.contributingReports === 1 ? '' : 's'}.`}
          </p>
          <p className="mt-1 text-sm text-stone-600">
            Last observed {formatWhen(crowd.lastObservedAt)} · source traveler reports · GPS proximity is a signal, not proof
          </p>
        </article>
        <div>
          <ExternalTrafficPanel community={traffic} external={externalTraffic} />
        </div>
      </section>

      <section className="rounded-2xl border border-line bg-white p-5">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <h2 className="text-2xl">Forecast</h2>
          <label className="text-sm">
            India date
            <input type="date" className="ml-2 rounded-lg border border-line px-2 py-1" value={forecastDate} onChange={(event) => setForecastDate(event.target.value)} />
          </label>
        </div>
        {forecast && (
          <div className="mt-4 space-y-3">
            <ul className="flex flex-wrap gap-2">
              {calendarFacts(forecast.calendarSignals).map((fact) => (
                <li key={fact} className="rounded-full bg-paper px-3 py-1 text-sm">{fact}</li>
              ))}
            </ul>
            {forecast.crowdEstimate.forecastStatus === 'INSUFFICIENT_DATA' ? (
              <p>
                <strong>Insufficient data.</strong> {forecast.crowdEstimate.message} Sample size {forecast.crowdEstimate.sampleSize}.
              </p>
            ) : (
              <p>
                <strong>Estimate, not a live observation.</strong> Intensity {forecast.crowdEstimate.estimatedIntensity} ({forecast.crowdEstimate.estimatedStatus}) from {forecast.crowdEstimate.sampleSize} same-weekday reports. {forecast.crowdEstimate.message}
              </p>
            )}
          </div>
        )}
      </section>

      <section>
        <h2 className="text-2xl">Alternatives</h2>
        <p className="mt-1 text-sm text-stone-600">
          Unknown crowd is never ranked as confirmed quiet. Confidence and freshness come from live verified reports only.
        </p>
        {alternatives.length === 0 ? (
          <p className="mt-3 text-stone-700">No nearby alternatives in range.</p>
        ) : (
          <ul className="mt-3 space-y-2">
            {alternatives.map((item) => (
              <li key={item.destination.id} className="rounded-2xl border border-line bg-white px-4 py-3">
                <Link to={`/destinations/${item.destination.id}`} className="font-medium">{item.destination.name}</Link>
                <p className="text-sm text-stone-600">
                  {item.distanceKm} km · {item.sameCategory ? 'same category' : 'other category'} · crowd{' '}
                  {item.crowdStatus.toLowerCase()}
                  {item.informationQuality === 'INSUFFICIENT_DATA'
                    ? ' · unknown — not enough data to call this quiet'
                    : ` · score ${item.crowdScore} · ${item.crowdConfidence.toLowerCase()} confidence`}
                  {item.lastObservedAt
                    ? ` · last observed ${formatWhen(item.lastObservedAt)}`
                    : ' · no recent reports'}
                  {item.source ? ` · source ${item.source === 'USER_REPORTS' ? 'traveler reports' : item.source}` : ''}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2 className="text-2xl">Nearby</h2>
        <ul className="mt-3 space-y-2">
          {nearby.map((item) => (
            <li key={item.destination.id}>
              <Link to={`/destinations/${item.destination.id}`}>{item.destination.name}</Link>
              <span className="text-sm text-stone-600"> · {item.distanceKm} km</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="rounded-2xl border border-line bg-white p-5">
        <h2 className="text-2xl">Report a visit</h2>
        {user ? (
          <form className="mt-4 grid gap-3 sm:grid-cols-2" onSubmit={submitReport}>
            <label className="text-sm">Crowd intensity 1–5
              <input
                className="mt-1 w-full rounded-lg border border-line px-3 py-2"
                type="number"
                min={1}
                max={5}
                step={1}
                required
                inputMode="numeric"
                value={intensity}
                onChange={(event) => setIntensity(event.target.value)}
              />
            </label>
            <label className="text-sm">Traffic
              <select className="mt-1 w-full rounded-lg border border-line px-3 py-2" value={trafficCondition} onChange={(event) => setTrafficCondition(event.target.value)}>
                <option value="LIGHT">Light</option>
                <option value="MODERATE">Moderate</option>
                <option value="HEAVY">Heavy</option>
                <option value="UNKNOWN">Unknown</option>
              </select>
            </label>
            <label className="text-sm">Wait minutes
              <input className="mt-1 w-full rounded-lg border border-line px-3 py-2" type="number" min={0} max={600} value={waitMinutes} onChange={(event) => setWaitMinutes(event.target.value)} />
            </label>
            <label className="text-sm">Observed at
              <input className="mt-1 w-full rounded-lg border border-line px-3 py-2" type="datetime-local" value={observedAt} onChange={(event) => setObservedAt(event.target.value)} />
            </label>
            <label className="text-sm sm:col-span-2">Note
              <textarea className="mt-1 w-full rounded-lg border border-line px-3 py-2" maxLength={500} value={note} onChange={(event) => setNote(event.target.value)} />
            </label>
            <label className="flex items-start gap-2 text-sm sm:col-span-2">
              <input
                className="mt-1"
                type="checkbox"
                checked={shareLocation}
                onChange={(event) => setShareLocation(event.target.checked)}
              />
              <span>
                Share my approximate location to verify I am near this place. This is a proximity check only — not proof the report is genuine.
                Precise coordinates stay on the server and are never shown publicly. Without sharing, the report is still accepted as pending and will not feed verified crowd/traffic.
              </span>
            </label>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-full bg-ink px-4 py-2 text-paper disabled:opacity-60 sm:col-span-2"
            >
              {submitting ? 'Submitting…' : 'Submit report'}
            </button>
            {reportMessage && <p className="text-sm sm:col-span-2">{reportMessage}</p>}
          </form>
        ) : (
          <p className="mt-3 text-sm">
            <Link to="/login" className="underline">Sign in</Link> to submit what you saw. You can file 10 reports per India day, with 30 minutes between reports for the same place. Location sharing is optional.
          </p>
        )}
      </section>
    </div>
  )
}
