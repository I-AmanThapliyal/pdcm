import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiFailure, pdcm } from '../api'
import { ConfidenceNote, CrowdBadge, TrafficBadge } from '../components/Badges'
import {
  popularUnknownNeedsReport,
  quieterEmptyCopy,
  recentReportsEmptyCopy,
} from '../dashboardStates'
import { calendarFacts, formatWhen } from '../format'
import {
  forecastQualityCopy,
  observationFreshness,
  providerAvailabilityCopy,
  sourceLabel,
  unknownCrowdCopy,
  unknownTrafficCopy,
} from '../freshness'
import { trustLabelText } from '../trustLabel'
import type { Dashboard, DestinationSnapshot } from '../types'

export function DashboardPage() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    pdcm
      .dashboard()
      .then((payload) => {
        if (!cancelled) {
          setDashboard(payload)
          setError(null)
        }
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(reason instanceof ApiFailure ? reason.message : 'The dashboard could not be loaded.')
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  if (error) {
    return (
      <section className="rounded-3xl border border-line bg-white p-6" role="alert">
        <h1 className="text-2xl">Dashboard unavailable</h1>
        <p className="mt-2 text-clay">{error}</p>
        <p className="mt-3 text-sm text-stone-600">Try again shortly. Missing data is never filled with synthetic crowd or traffic.</p>
      </section>
    )
  }

  if (!dashboard) {
    return (
      <section className="rounded-3xl border border-line bg-white p-6" aria-busy="true">
        <p className="text-stone-700">Loading advisory…</p>
        <p className="mt-2 text-sm text-stone-600">Crowd, community traffic, and forecast quality load from the public dashboard API.</p>
      </section>
    )
  }

  const windowHours = dashboard.observationWindowHours ?? 6
  const quieterEmpty = quieterEmptyCopy(dashboard.quieterKnownDestinations)
  const reportsEmpty = recentReportsEmptyCopy(dashboard.recentReports.length)
  const provider = dashboard.externalTrafficProvider
  const coverage = dashboard.catalogueCoverage

  return (
    <div className="space-y-10">
      <section className="rounded-3xl border border-line bg-white p-6">
        <p className="text-sm uppercase tracking-wide text-stone-500">Visit advisory</p>
        <h1 className="mt-1 text-4xl">Should you go now?</h1>
        <p className="mt-2 max-w-3xl text-stone-700">
          Live crowd and community traffic use location-verified traveler reports from the last {windowHours} hours.
          Forecasts are historical estimates. Calendar facts are context only. External road traffic is a separate channel.
        </p>
        {coverage && (
          <p className="mt-4 text-sm text-stone-600">
            Catalogue: {coverage.activeDestinationCount} active destinations across {coverage.regionCount} regions
            {coverage.regions.length > 0 ? ` (${coverage.regions.slice(0, 6).join(', ')}${coverage.regions.length > 6 ? '…' : ''})` : ''}.
          </p>
        )}
        {provider && (
          <p className="mt-2 rounded-xl bg-paper px-4 py-3 text-sm text-stone-700" role="status">
            {providerAvailabilityCopy(provider)}
          </p>
        )}
      </section>

      <section className="rounded-3xl border border-line bg-white p-6">
        <p className="text-sm uppercase tracking-wide text-stone-500">Calendar context</p>
        <h2 className="mt-1 text-3xl">{dashboard.calendarHighlight.targetDate}</h2>
        <p className="mt-2 text-lg">
          {dashboard.calendarHighlight.label === 'LONG_WEEKEND' ? 'Long weekend' : 'Weekend'} — not a crowd observation
        </p>
        <ul className="mt-4 flex flex-wrap gap-2">
          {calendarFacts(dashboard.calendarHighlight.calendarSignals).map((fact) => (
            <li key={fact} className="rounded-full bg-paper px-3 py-1 text-sm">{fact}</li>
          ))}
        </ul>
      </section>

      <section>
        <h2 className="text-2xl">Popular places</h2>
        <p className="mt-1 text-sm text-stone-600">
          Ranked by report activity in the last 7 days (all statuses). Live badges use only location-verified reports
          from the last {windowHours} hours — popularity is not verified crowd.
        </p>
        <div className="mt-4 grid gap-4 lg:grid-cols-2">
          {dashboard.popularDestinations.map((item) => (
            <DestinationQualityCard
              key={item.destination.id}
              item={item}
              asOf={dashboard.asOf}
              windowHours={windowHours}
            />
          ))}
        </div>
      </section>

      <section>
        <h2 className="text-2xl">Quieter known places</h2>
        <p className="mt-1 text-sm text-stone-600">
          Only destinations with a known live crowd score. Unknown confidence is never ranked as quiet.
        </p>
        {quieterEmpty ? (
          <p className="mt-3 rounded-2xl border border-line bg-white px-4 py-3 text-stone-700">{quieterEmpty}</p>
        ) : (
          <ul className="mt-4 space-y-3">
            {dashboard.quieterKnownDestinations.map((item) => {
              const freshness = observationFreshness(item.crowd.lastObservedAt, dashboard.asOf, windowHours)
              return (
                <li key={item.destination.id}>
                  <Link
                    to={`/destinations/${item.destination.id}`}
                    className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-line bg-white px-4 py-3"
                  >
                    <span>
                      <span className="font-medium">{item.destination.name}</span>
                      <span className="mt-1 block text-sm text-stone-600">
                        {freshness.label} · {sourceLabel(item.crowd.source)} · score {item.crowd.score}
                      </span>
                    </span>
                    <span className="flex items-center gap-2">
                      <CrowdBadge status={item.crowd.status} />
                      <ConfidenceNote confidence={item.crowd.confidence} />
                    </span>
                  </Link>
                </li>
              )
            })}
          </ul>
        )}
      </section>

      <section>
        <h2 className="text-2xl">Recent reports</h2>
        <p className="mt-1 text-sm text-stone-600">
          Trust labels describe verification state only. Only location-verified signals feed live crowd and community traffic.
        </p>
        {reportsEmpty ? (
          <p className="mt-3 rounded-2xl border border-line bg-white px-4 py-3 text-stone-700">{reportsEmpty}</p>
        ) : (
          <ul className="mt-4 divide-y divide-line rounded-2xl border border-line bg-white">
            {dashboard.recentReports.map((report) => (
              <li key={report.id} className="flex flex-wrap items-center justify-between gap-2 px-4 py-3 text-sm">
                <Link to={`/destinations/${report.destinationId}`} className="font-medium">
                  {report.destinationName}
                </Link>
                <span>
                  Crowd {report.crowdIntensity}/5 · traffic {report.trafficCondition.toLowerCase()}
                  {' · '}
                  {trustLabelText(report.trustLabel, report.verificationStatus)}
                </span>
                <span className="text-stone-600">{formatWhen(report.observedAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}

function DestinationQualityCard({
  item,
  asOf,
  windowHours,
}: {
  item: DestinationSnapshot
  asOf: string
  windowHours: number
}) {
  const crowdFreshness = observationFreshness(item.crowd.lastObservedAt, asOf, windowHours)
  const trafficFreshness = observationFreshness(item.traffic.lastObservedAt, asOf, windowHours)
  const needsReport = popularUnknownNeedsReport(item)
  const forecast = item.forecastQuality

  return (
    <article className="rounded-2xl border border-line bg-white p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-xl">
            <Link to={`/destinations/${item.destination.id}`} className="hover:underline">
              {item.destination.name}
            </Link>
          </h3>
          <p className="text-sm text-stone-600">
            {item.destination.region}
            {item.destination.city ? ` · ${item.destination.city}` : ''}
            {' · '}
            {item.recentReportCount} reports this week
            {typeof item.verifiedRecentReportCount === 'number'
              ? ` · ${item.verifiedRecentReportCount} location-verified`
              : ''}
          </p>
        </div>
        <CrowdBadge status={item.crowd.status} />
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <div className="rounded-xl bg-paper px-3 py-3">
          <p className="text-xs uppercase tracking-wide text-stone-500">Attraction crowd</p>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <CrowdBadge status={item.crowd.status} />
            <ConfidenceNote confidence={item.crowd.confidence} />
          </div>
          <p className="mt-2 text-sm text-stone-700">
            {item.crowd.status === 'UNKNOWN'
              ? unknownCrowdCopy()
              : `${item.crowd.contributingReports} verified report${item.crowd.contributingReports === 1 ? '' : 's'} · score ${item.crowd.score}`}
          </p>
          <p className="mt-1 text-sm text-stone-600">
            {crowdFreshness.label} · source {sourceLabel(item.crowd.source)}
          </p>
        </div>

        <div className="rounded-xl bg-paper px-3 py-3">
          <p className="text-xs uppercase tracking-wide text-stone-500">Community visitor traffic</p>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <TrafficBadge status={item.traffic.status} />
            <ConfidenceNote confidence={item.traffic.confidence} />
          </div>
          <p className="mt-2 text-sm text-stone-700">
            {item.traffic.status === 'UNKNOWN'
              ? unknownTrafficCopy()
              : `${item.traffic.contributingReports} verified report${item.traffic.contributingReports === 1 ? '' : 's'}`}
          </p>
          <p className="mt-1 text-sm text-stone-600">
            {trafficFreshness.label} · source {sourceLabel(item.traffic.source)}
          </p>
          <p className="mt-2 text-xs text-stone-500">Not external road data — that channel is separate and pilot-only.</p>
        </div>
      </div>

      {forecast && (
        <p className="mt-3 text-sm text-stone-700">{forecastQualityCopy(forecast)}</p>
      )}

      {needsReport && (
        <p className="mt-3 text-sm">
          <Link to={`/destinations/${item.destination.id}`} className="font-medium underline">
            Report what you see
          </Link>
          <span className="text-stone-600"> — helps turn unknown into a verified signal.</span>
        </p>
      )}

      <p className="mt-3">
        <Link to={`/destinations/${item.destination.id}`} className="text-sm font-medium underline">
          Open detail, forecast, and alternatives
        </Link>
      </p>
    </article>
  )
}
