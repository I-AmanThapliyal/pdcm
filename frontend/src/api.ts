import type {
  AdminReportDetail,
  AdminReportSummary,
  Alternative,
  AuthResult,
  CreateDestinationSuggestionBody,
  CrowdSummary,
  Dashboard,
  Destination,
  DestinationSuggestion,
  ExternalTrafficSummary,
  Forecast,
  Nearby,
  Page,
  ProviderTrafficStatus,
  SuggestionStatus,
  TrafficSummary,
  UserProfile,
  ApiProblem,
  VerificationStatus,
} from './types'

const TOKEN_KEY = 'pdcm.token'

export class ApiFailure extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly code?: string,
    readonly details: string[] = [],
  ) {
    super(message)
  }
}

export function token(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function saveToken(value: string | null) {
  if (value) localStorage.setItem(TOKEN_KEY, value)
  else localStorage.removeItem(TOKEN_KEY)
}

export async function api<T>(path: string, init: RequestInit & { auth?: boolean } = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (init.auth) {
    const bearer = token()
    if (bearer) headers.set('Authorization', `Bearer ${bearer}`)
  }
  const response = await fetch(path, { ...init, headers })
  if (!response.ok) {
    let problem: ApiProblem = {}
    try {
      problem = (await response.json()) as ApiProblem
    } catch {
      problem = {}
    }
    if (response.status === 401) saveToken(null)
    throw new ApiFailure(
      response.status,
      problem.message ?? response.statusText,
      problem.code,
      problem.details ?? [],
    )
  }
  return (await response.json()) as T
}

export const pdcm = {
  dashboard: () => api<Dashboard>('/api/dashboard'),
  destinations: (params: URLSearchParams) => api<Page<Destination>>(`/api/destinations?${params}`),
  destinationsMap: (params: URLSearchParams) => api<Destination[]>(`/api/destinations/map?${params}`),
  destination: (id: string) => api<Destination>(`/api/destinations/${id}`),
  crowd: (id: string) => api<CrowdSummary>(`/api/destinations/${id}/crowd`),
  traffic: (id: string) => api<TrafficSummary>(`/api/destinations/${id}/traffic`),
  trafficExternal: (id: string) => api<ExternalTrafficSummary>(`/api/destinations/${id}/traffic/external`),
  providerTrafficStatus: () => api<ProviderTrafficStatus>('/api/providers/traffic/status'),
  /** ADMIN — usage counters and remaining free-tier budget. */
  adminProviderTrafficUsage: () =>
    api<ProviderTrafficStatus>('/api/admin/providers/traffic/usage', { auth: true }),

  forecast: (id: string, date: string) =>
    api<Forecast>(`/api/destinations/${id}/forecast?date=${encodeURIComponent(date)}`),
  alternatives: (id: string) => api<Alternative[]>(`/api/destinations/${id}/alternatives?radiusKm=150&limit=5`),
  nearby: (id: string) => api<Nearby[]>(`/api/destinations/${id}/nearby?radiusKm=100&limit=5`),
  register: (body: { email: string; displayName: string; password: string }) =>
    api<AuthResult>('/api/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  login: (body: { email: string; password: string }) =>
    api<AuthResult>('/api/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  me: () => api<UserProfile>('/api/me', { auth: true }),
  report: (
    id: string,
    body: {
      crowdIntensity: number
      trafficCondition: string
      waitMinutes: number | null
      note: string | null
      observedAt: string
      locationConsent?: boolean
      clientLocation?: {
        latitude: number
        longitude: number
        accuracyMeters: number | null
      } | null
    },
  ) =>
    api<{
      id: number
      verificationStatus: string
      verificationReason: string | null
      trustLabel?: string
    }>(`/api/destinations/${id}/reports`, { method: 'POST', auth: true, body: JSON.stringify(body) }),
  adminReports: (status: VerificationStatus = 'FLAGGED', limit = 50) =>
    api<AdminReportSummary[]>(
      `/api/admin/reports?status=${encodeURIComponent(status)}&limit=${limit}`,
      { auth: true },
    ),
  adminReport: (id: number, includeCoordinates = false) =>
    api<AdminReportDetail>(
      `/api/admin/reports/${id}?includeCoordinates=${includeCoordinates}`,
      { auth: true },
    ),
  adminVerifyReport: (id: number, note?: string) =>
    api<AdminReportDetail>(`/api/admin/reports/${id}/verify`, {
      method: 'POST',
      auth: true,
      body: JSON.stringify({ note: note ?? null }),
    }),
  adminRejectReport: (id: number, note?: string) =>
    api<AdminReportDetail>(`/api/admin/reports/${id}/reject`, {
      method: 'POST',
      auth: true,
      body: JSON.stringify({ note: note ?? null }),
    }),
  adminFlagReport: (id: number, note?: string) =>
    api<AdminReportDetail>(`/api/admin/reports/${id}/flag`, {
      method: 'POST',
      auth: true,
      body: JSON.stringify({ note: note ?? null }),
    }),
  submitDestinationSuggestion: (body: CreateDestinationSuggestionBody) =>
    api<DestinationSuggestion>('/api/destination-suggestions', {
      method: 'POST',
      auth: true,
      body: JSON.stringify(body),
    }),
  myDestinationSuggestions: () =>
    api<DestinationSuggestion[]>('/api/me/destination-suggestions', { auth: true }),
  myDestinationSuggestion: (id: number) =>
    api<DestinationSuggestion>(`/api/me/destination-suggestions/${id}`, { auth: true }),
  adminDestinationSuggestions: (status: SuggestionStatus = 'SUBMITTED', limit = 50) =>
    api<DestinationSuggestion[]>(
      `/api/admin/destination-suggestions?status=${encodeURIComponent(status)}&limit=${limit}`,
      { auth: true },
    ),
  adminDestinationSuggestion: (id: number) =>
    api<DestinationSuggestion>(`/api/admin/destination-suggestions/${id}`, { auth: true }),
  adminUpdateSuggestionCoordinates: (id: number, latitude: number, longitude: number) =>
    api<DestinationSuggestion>(`/api/admin/destination-suggestions/${id}/coordinates`, {
      method: 'PATCH',
      auth: true,
      body: JSON.stringify({ latitude, longitude }),
    }),
  adminApproveDestinationSuggestion: (id: number, note?: string) =>
    api<DestinationSuggestion>(`/api/admin/destination-suggestions/${id}/approve`, {
      method: 'POST',
      auth: true,
      body: JSON.stringify({ note: note ?? null }),
    }),
  adminRejectDestinationSuggestion: (id: number, note: string) =>
    api<DestinationSuggestion>(`/api/admin/destination-suggestions/${id}/reject`, {
      method: 'POST',
      auth: true,
      body: JSON.stringify({ note }),
    }),
}
