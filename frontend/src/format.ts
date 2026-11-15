import type { CalendarSignals, Confidence, CrowdStatus, ExternalTrafficStatus, TrafficStatus } from './types'

export function indiaDate(offsetDays = 0): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Kolkata',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const year = Number(parts.find((part) => part.type === 'year')?.value)
  const month = Number(parts.find((part) => part.type === 'month')?.value)
  const day = Number(parts.find((part) => part.type === 'day')?.value)
  const date = new Date(Date.UTC(year, month - 1, day + offsetDays))
  return date.toISOString().slice(0, 10)
}

export function formatWhen(iso: string | null): string {
  if (!iso) return 'No observation time'
  return new Intl.DateTimeFormat('en-IN', {
    timeZone: 'Asia/Kolkata',
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(iso))
}

export function crowdLabel(status: CrowdStatus): string {
  if (status === 'UNKNOWN') return 'Unknown'
  return status.charAt(0) + status.slice(1).toLowerCase()
}

export function trafficLabel(status: TrafficStatus): string {
  if (status === 'UNKNOWN') return 'Unknown'
  return status.charAt(0) + status.slice(1).toLowerCase()
}

export function externalTrafficLabel(status: ExternalTrafficStatus): string {
  if (status === 'UNAVAILABLE') return 'Unavailable'
  return trafficLabel(status)
}

export function externalTrafficTone(status: ExternalTrafficStatus): string {
  if (status === 'UNAVAILABLE') return 'bg-stone-200 text-stone-800'
  return trafficTone(status)
}

export function confidenceLabel(confidence: Confidence): string {
  if (confidence === 'NONE') return 'No confidence'
  return `${confidence.charAt(0)}${confidence.slice(1).toLowerCase()} confidence`
}

export function calendarFacts(signals: CalendarSignals): string[] {
  const facts = [
    signals.isWeekend ? 'Weekend' : 'Weekday',
    signals.isNationalHoliday ? signals.holidayName ?? 'National holiday' : 'Not a national holiday',
    signals.isLongWeekend ? 'Long weekend' : 'Not a long weekend',
  ]
  return facts
}

export function localDateTimeValue(date = new Date()): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function crowdTone(status: CrowdStatus): string {
  switch (status) {
    case 'QUIET':
      return 'bg-lime-100 text-lime-950'
    case 'MODERATE':
      return 'bg-amber-100 text-amber-950'
    case 'BUSY':
      return 'bg-orange-200 text-orange-950'
    case 'CROWDED':
    case 'OVERCROWDED':
      return 'bg-red-200 text-red-950'
    default:
      return 'bg-stone-200 text-stone-800'
  }
}

export function trafficTone(status: TrafficStatus): string {
  switch (status) {
    case 'LIGHT':
      return 'bg-lime-100 text-lime-950'
    case 'MODERATE':
      return 'bg-amber-100 text-amber-950'
    case 'HEAVY':
      return 'bg-red-200 text-red-950'
    default:
      return 'bg-stone-200 text-stone-800'
  }
}
