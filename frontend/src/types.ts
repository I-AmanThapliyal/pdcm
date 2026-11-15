export type Confidence = 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH'
export type CrowdStatus = 'UNKNOWN' | 'QUIET' | 'MODERATE' | 'BUSY' | 'CROWDED' | 'OVERCROWDED'
export type TrafficStatus = 'UNKNOWN' | 'LIGHT' | 'MODERATE' | 'HEAVY'
export type Category =
  | 'HISTORICAL'
  | 'TEMPLE'
  | 'BEACH'
  | 'HILL_STATION'
  | 'WILDLIFE'
  | 'CITY'
  | 'NATURE'

export interface Destination {
  id: number
  name: string
  slug: string
  description: string
  region: string
  city?: string | null
  category: Category
  latitude: number
  longitude: number
  crowdSensitivity: string
  sourceReference?: string | null
  sourceUrl?: string | null
  sourceLicense?: string | null
  tags: string[]
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface CrowdSummary {
  destinationId: number
  status: CrowdStatus
  score: number | null
  confidence: Confidence
  contributingReports: number
  lastObservedAt: string | null
  asOf: string
  source: string
}

export interface TrafficSummary {
  destinationId: number
  status: TrafficStatus
  confidence: Confidence
  contributingReports: number
  lastObservedAt: string | null
  asOf: string
  source: string
}

export type ExternalTrafficStatus = TrafficStatus | 'UNAVAILABLE'
export type ExternalFreshness = 'FRESH' | 'STALE' | 'NONE'

export interface ExternalTrafficSummary {
  destinationId: number
  channel: 'EXTERNAL_ROAD'
  provider: string
  status: ExternalTrafficStatus
  currentSpeedKph: number | null
  freeFlowSpeedKph: number | null
  providerConfidence: number | null
  functionalRoadClass: string | null
  fetchedAt: string
  expiresAt: string
  freshness: ExternalFreshness
  attribution: string
  message: string | null
  pilotEnabled: boolean
  coverageAvailable: boolean
  coverageNote: string | null
  interpretation: string | null
}

export interface ProviderTrafficStatus {
  provider: string
  status: 'UP' | 'DEGRADED' | 'DOWN' | 'DISABLED'
  configured: boolean
  scheduleEnabled: boolean
  incidentsEnabled: boolean
  cacheTtlMinutes: number
  dailyRequestBudget: number
  monthlyRequestBudget: number
  requestsToday: number
  requestsThisMonth: number
  remainingDailyBudget: number
  remainingMonthlyBudget: number
  successesToday: number
  failuresToday: number
  budgetSkippedToday: number
  lastSuccessAt: string | null
  lastFailureAt: string | null
  lastErrorCode: string | null
  consecutiveFailures: number
  pilotSlugs: string[]
  flowZoomFallbacks: number[]
  usageNote: string
}

export interface DashboardForecastQuality {
  targetDate: string
  forecastStatus: 'INSUFFICIENT_DATA' | 'ESTIMATE'
  estimatedIntensity: number | null
  estimatedStatus: CrowdStatus | null
  confidence: Confidence
  sampleSize: number
  message: string
}

export interface CatalogueCoverage {
  activeDestinationCount: number
  regionCount: number
  regions: string[]
}

export interface DestinationSnapshot {
  destination: Destination
  crowd: CrowdSummary
  traffic: TrafficSummary
  /** Activity/popularity — all report statuses in the last 7 days. */
  recentReportCount: number
  /** LOCATION_VERIFIED reports in the last 7 days (not live crowd). */
  verifiedRecentReportCount: number
  /** Historical estimate for tomorrow — never a live observation. */
  forecastQuality: DashboardForecastQuality
}

export interface CalendarSignals {
  isWeekend: boolean
  isNationalHoliday: boolean
  isLongWeekend: boolean
  holidayName: string | null
}

export interface Dashboard {
  asOf: string
  observationWindowHours: number
  catalogueCoverage: CatalogueCoverage
  /** Display-only provider health — dashboard does not call TomTom Flow. */
  externalTrafficProvider: ProviderTrafficStatus
  popularDestinations: DestinationSnapshot[]
  calendarHighlight: {
    targetDate: string
    calendarSignals: CalendarSignals
    label: 'LONG_WEEKEND' | 'WEEKEND'
  }
  recentReports: RecentReport[]
  quieterKnownDestinations: DestinationSnapshot[]
}

export type VerificationStatus =
  | 'LEGACY_UNVERIFIED'
  | 'PENDING'
  | 'LOCATION_VERIFIED'
  | 'FLAGGED'
  | 'REJECTED'

export type ReportTrustLabel =
  | 'LOCATION_VERIFIED_SIGNAL'
  | 'PENDING_REVIEW'
  | 'FLAGGED_FOR_REVIEW'
  | 'REJECTED'
  | 'LEGACY_UNVERIFIED'

export interface RecentReport {
  id: number
  destinationId: number
  destinationName: string
  crowdIntensity: number
  trafficCondition: TrafficStatus
  observedAt: string
  createdAt: string
  verificationStatus?: VerificationStatus
  verificationReason?: string | null
  trustLabel?: ReportTrustLabel
}

export interface AdminReportSummary {
  id: number
  destinationId: number
  destinationName: string
  contributorUserId: number
  crowdIntensity: number
  trafficCondition: string
  waitMinutes: number | null
  note: string | null
  observedAt: string
  createdAt: string
  verificationStatus: VerificationStatus
  verificationReason: string | null
  trustLabel: ReportTrustLabel
  distanceToDestinationMeters: number | null
  coordinatesRetained: boolean
  locationConsentAt: string | null
  locationPurgedAt: string | null
}

export interface ModerationEvent {
  id: number
  reportId: number
  actorUserId: number
  fromStatus: VerificationStatus
  toStatus: VerificationStatus
  note: string | null
  createdAt: string
}

export interface AdminReportDetail {
  report: AdminReportSummary
  retainedCoordinates: {
    latitude: number
    longitude: number
    accuracyMeters: number | null
  } | null
  moderationHistory: ModerationEvent[]
}

export interface Forecast {
  destinationId: number
  targetDate: string
  calendarSignals: CalendarSignals
  crowdEstimate: {
    forecastStatus: 'INSUFFICIENT_DATA' | 'ESTIMATE'
    estimatedIntensity: number | null
    estimatedStatus: CrowdStatus | null
    confidence: Confidence
    label: string
    sampleSize: number
    message: string
  }
  asOf: string
}

export interface Alternative {
  destination: Destination
  distanceKm: number
  sameCategory: boolean
  crowdStatus: CrowdStatus
  crowdScore: number | null
  crowdConfidence: Confidence
  informationQuality: 'INSUFFICIENT_DATA' | 'SUFFICIENT'
  contributingReports: number
  lastObservedAt: string | null
  source: string
}

export interface Nearby {
  destination: Destination
  distanceKm: number
}

export interface UserProfile {
  id: number
  email: string
  displayName: string
  role: 'USER' | 'ADMIN'
  createdAt: string
}

export interface AuthResult {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  user: UserProfile
}

export interface ApiProblem {
  code?: string
  message?: string
  details?: string[]
}

export type SuggestionStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED'

export interface PossibleDuplicate {
  destinationId: number
  name: string
  slug: string
  region: string
  city?: string | null
  category: Category
  latitude: number
  longitude: number
  matchReason: string
  distanceKm: number | null
}

export interface SuggestionEvent {
  id: number
  suggestionId: number
  actorUserId: number
  action: 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'COORDINATES_UPDATED'
  fromStatus: SuggestionStatus
  toStatus: SuggestionStatus
  note: string | null
  createdAt: string
}

export interface DestinationSuggestion {
  id: number
  name: string
  city: string
  region: string
  category: Category
  description: string
  latitude: number | null
  longitude: number | null
  referenceUrl: string | null
  status: SuggestionStatus
  adminNote: string | null
  createdDestinationId: number | null
  possibleDuplicate: boolean
  possibleDuplicates: PossibleDuplicate[]
  contributorUserId?: number | null
  auditHistory?: SuggestionEvent[]
  createdAt: string
  updatedAt: string
}

export interface CreateDestinationSuggestionBody {
  name: string
  city: string
  region: string
  category: Category
  description: string
  latitude?: number | null
  longitude?: number | null
  referenceUrl?: string | null
}
