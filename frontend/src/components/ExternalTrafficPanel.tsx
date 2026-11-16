import { ExternalTrafficBadge, TrafficBadge, ConfidenceNote } from './Badges'
import { formatWhen } from '../format'
import {
  externalCoverageDetail,
  externalCoverageLabel,
  externalCoverageVisibility,
} from '../externalTrafficCoverage'
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

  const visibility = external
    ? externalCoverageVisibility({
        pilotEnabled: external.pilotEnabled,
        coverageAvailable: external.coverageAvailable,
        status: external.status,
        freshness: external.freshness,
      })
    : null

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
        {external && visibility && (
          <>
            <div className="mt-3 flex flex-wrap items-center gap-2">
              <ExternalTrafficBadge status={external.status} />
              <span className="rounded-full bg-stone-100 px-2.5 py-1 text-sm font-medium text-stone-800">
                Coverage: {externalCoverageLabel(visibility)}
              </span>
              <span className="text-sm text-stone-600">
                {external.freshness === 'STALE'
                  ? 'Stale cache'
                  : external.freshness === 'FRESH'
                    ? 'Fresh'
                    : 'No sample'}
                {' · '}
                Source: {external.provider === 'TOMTOM' ? 'TomTom' : external.provider}
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
              {externalCoverageDetail({
                visibility,
                pilotEnabled: external.pilotEnabled,
                coverageNote: external.coverageNote,
                message: external.message,
              })}
            </p>
            <p className="mt-1 text-sm text-stone-600">
              Last fetched {formatWhen(external.fetchedAt)} · {external.attribution}
            </p>
            <p className="mt-2 text-xs text-stone-500">
              Provider availability is not destination-wide road coverage. PDCM samples one nearby approach road when the place is in the TomTom pilot.
            </p>
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
