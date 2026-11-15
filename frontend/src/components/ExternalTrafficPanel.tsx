import { ExternalTrafficBadge, TrafficBadge, ConfidenceNote } from './Badges'
import { formatWhen } from '../format'
import type { ExternalTrafficSummary, TrafficSummary } from '../types'

export function ExternalTrafficPanel({
  community,
  external,
}: {
  community: TrafficSummary
  external: ExternalTrafficSummary | null
}) {
  const disagree =
    external
    && external.status !== 'UNAVAILABLE'
    && external.status !== 'UNKNOWN'
    && community.status !== 'UNKNOWN'
    && community.status !== external.status

  return (
    <div className="space-y-4">
      <article className="rounded-2xl border border-line bg-white p-5">
        <h2 className="text-2xl">Traffic on site</h2>
        <p className="mt-1 text-sm text-stone-600">Community traveler reports · source {community.source}</p>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <TrafficBadge status={community.status} />
          <ConfidenceNote confidence={community.confidence} />
        </div>
        <p className="mt-3 text-sm">
          {community.status === 'UNKNOWN'
            ? 'No location-verified traffic reports in the last 6 hours. Unknown is not quiet.'
            : `${community.contributingReports} location-verified report${community.contributingReports === 1 ? '' : 's'}.`}
        </p>
        <p className="mt-1 text-sm text-stone-600">Last observed {formatWhen(community.lastObservedAt)}</p>
      </article>

      <article className="rounded-2xl border border-line bg-white p-5">
        <h2 className="text-2xl">Approach roads</h2>
        <p className="mt-1 text-sm text-stone-600">External road traffic · never treated as attraction crowd</p>
        {!external && <p className="mt-3 text-sm text-stone-600">Loading external traffic…</p>}
        {external && (
          <>
            <div className="mt-3 flex flex-wrap items-center gap-2">
              <ExternalTrafficBadge status={external.status} />
              <span className="text-sm text-stone-600">
                {external.freshness.toLowerCase()} · Source: {external.provider === 'TOMTOM' ? 'TomTom' : external.provider}
              </span>
            </div>
            {external.status === 'UNAVAILABLE' || external.status === 'UNKNOWN' ? (
              <p className="mt-3 text-sm text-stone-600">
                {external.message ?? 'No usable external road traffic for this place yet.'}
              </p>
            ) : (
              <>
                <p className="mt-3 text-sm">
                  {external.currentSpeedKph != null && external.freeFlowSpeedKph != null
                    ? `Current ${external.currentSpeedKph} km/h · free-flow ${external.freeFlowSpeedKph} km/h`
                    : 'Speed details unavailable.'}
                  {external.providerConfidence != null
                    ? ` · provider confidence ${(external.providerConfidence * 100).toFixed(0)}%`
                    : ''}
                </p>
                {external.interpretation && (
                  <p className="mt-2 text-sm text-stone-700">{external.interpretation}</p>
                )}
              </>
            )}
            <p className="mt-2 text-sm text-stone-600">
              Coverage: {external.coverageAvailable
                ? (external.coverageNote ?? 'Nearby approach road sample available.')
                : 'No suitable nearby road sample available.'}
            </p>
            <p className="mt-1 text-sm text-stone-600">
              Last fetched {formatWhen(external.fetchedAt)} · freshness {external.freshness.toLowerCase()} · {external.attribution}
            </p>
            {!external.pilotEnabled && (
              <p className="mt-2 text-sm text-stone-600">Outside the five-destination TomTom pilot.</p>
            )}
          </>
        )}
      </article>

      {disagree && (
        <p className="rounded-xl border border-line bg-paper px-4 py-3 text-sm" role="status">
          Road data and traveler reports disagree. Compare freshness and decide — they are not merged into one score.
        </p>
      )}
    </div>
  )
}
