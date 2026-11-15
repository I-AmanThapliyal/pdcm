import {
  confidenceLabel,
  crowdLabel,
  crowdTone,
  externalTrafficLabel,
  externalTrafficTone,
  trafficLabel,
  trafficTone,
} from '../format'
import type { Confidence, CrowdStatus, ExternalTrafficStatus, TrafficStatus } from '../types'

export function CrowdBadge({ status }: { status: CrowdStatus }) {
  return <span className={`rounded-full px-2.5 py-1 text-sm font-medium ${crowdTone(status)}`}>{crowdLabel(status)}</span>
}

export function TrafficBadge({ status }: { status: TrafficStatus }) {
  return <span className={`rounded-full px-2.5 py-1 text-sm font-medium ${trafficTone(status)}`}>{trafficLabel(status)}</span>
}

export function ExternalTrafficBadge({ status }: { status: ExternalTrafficStatus }) {
  return (
    <span className={`rounded-full px-2.5 py-1 text-sm font-medium ${externalTrafficTone(status)}`}>
      {externalTrafficLabel(status)}
    </span>
  )
}

export function ConfidenceNote({ confidence }: { confidence: Confidence }) {
  return <span className="text-sm text-stone-600">{confidenceLabel(confidence)}</span>
}
