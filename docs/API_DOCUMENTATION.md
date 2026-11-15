# API Documentation

Base path: `/api`. JSON. Timestamps: **ISO-8601 UTC** ending in `Z`.

Auth: `Authorization: Bearer <access_token>` where noted. Access token ~60 minutes, HS256. No refresh token in MVP.

Interactive docs (Phase 1+): `/swagger-ui.html`, OpenAPI `/v3/api-docs`.

## Auth

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/auth/register` | public | email, password, displayName → user + token |
| POST | `/auth/login` | public | email, password → token; generic 401 on failure |
| GET | `/me` | USER+ | profile (no password hash) |

## Destinations

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/destinations` | public | search `q`, filter `region`, `category`; also matches `city`; stable `PageResponse` |
| GET | `/destinations/map` | public | destinations inside map bounds; same `q` / `region` / `category` filters; capped list |
| GET | `/destinations/{id}` | public | detail + tags + coords + city + provenance |
| GET | `/destinations/{id}/nearby` | public | Haversine; `radiusKm` default 100, max **500**; `limit` 1–50 |
| POST | `/destinations` | ADMIN | **not implemented yet** (Day later) |
| PUT | `/destinations/{id}` | ADMIN | **not implemented yet** |
| PATCH | `/destinations/{id}/active` | ADMIN | **not implemented yet** |

### Map bounds (`GET /destinations/map`)

Required query: `south`, `west`, `north`, `east` (WGS84 degrees). Optional: `q`, `region`, `category`, `limit` (default **100**, max **200**).

Validation (400 `VALIDATION_ERROR`):

- Coordinates must be in range; `south < north`; `west < east` (no antimeridian wrap).
- Each axis span must be ≤ **40°**.
- `limit` must be 1–200.

Response: JSON array of the same destination objects as list/detail (including `city`, `sourceReference`, `sourceUrl`, `sourceLicense`). Does **not** embed crowd or traffic — clients call existing `/crowd` and `/traffic` only for a selected place.

### PageResponse shape

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

## Reports (Day 2)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/destinations/{id}/reports` | USER/ADMIN | submit VisitReport; user from JWT, not body |
| GET | `/destinations/{id}/reports` | public | **not implemented yet** (Day 3+) |

**Request body (Phase 3 GPS fields optional):**

```json
{
  "crowdIntensity": 3,
  "trafficCondition": "MODERATE",
  "waitMinutes": null,
  "note": null,
  "observedAt": "2026-11-14T12:00:00Z",
  "locationConsent": true,
  "clientLocation": {
    "latitude": 26.9855,
    "longitude": 75.8513,
    "accuracyMeters": 12.0
  }
}
```

- `locationConsent` omitted/false → report accepted as `PENDING` / `NO_CONSENT`; any `clientLocation` is ignored (privacy).  
- Consent true but GPS unavailable/denied → `PENDING` / `LOCATION_UNAVAILABLE`.  
- Consent + accurate GPS within geofence → `LOCATION_VERIFIED` / `WITHIN_GEOFENCE` (proximity signal only — **not** proof the report is genuine).  
- Response includes `verificationStatus`, `verificationReason`, and public-safe `trustLabel`. **Never** returns client lat/lon/accuracy.

**`observedAt` validation (400 if invalid):**

- Must not be more than **60 seconds** in the future (clock skew).  
- Must not be older than **24 hours** before server now.  
- Cooldown (30 min) and daily cap (10 / India day) are enforced with **`created_at`**, not `observedAt`.

**429** when cooldown or daily cap hit:

```json
{
  "code": "REPORT_RATE_LIMITED",
  "message": "Too many reports. Try again later.",
  "limit": "COOLDOWN_30M | DAILY_10"
}
```

Daily cap: 10 reports per user per India calendar day. Cooldown: 30 minutes per destination. Existing caps/cooldowns/advisory locks are unchanged.

## Crowd & traffic (Day 3)

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/destinations/{id}/crowd` | public | current aggregation from **LOCATION_VERIFIED** reports only; none → `UNKNOWN`, not quiet |
| GET | `/destinations/{id}/traffic` | public | **community** mode from **LOCATION_VERIFIED** reports; `UNKNOWN` traffic ignored |
| GET | `/destinations/{id}/traffic/external` | public | **external road** channel (TomTom Flow pilot); never merged with community |
| GET | `/providers/traffic/status` | public | provider health + daily/monthly request metadata (no secrets) |
| GET | `/admin/providers/traffic/usage` | ADMIN | same usage counters + remaining free-tier budget for operators |

### External traffic response (shape)

Separate channel from community traffic. Pilot destinations call TomTom Flow Segment Data **on demand** (server-side) when a destination detail/external endpoint is opened. Observations are **in-memory cached** (default **180 minutes / 3 hours**, shared per destination). Concurrent opens for the same destination share one provider fetch (single-flight). Zoom fallback **18 → 15 → 12 → 10** stops at the first geographically valid segment; each attempt that hits TomTom counts against budget. Map tiles remain OSM/Leaflet — not TomTom tiles. TomTom payloads are **not** persisted in Postgres (licence-safe short TTL cache only). Traffic Incidents are **not** implemented.

```json
{
  "destinationId": 1,
  "channel": "EXTERNAL_ROAD",
  "provider": "TOMTOM",
  "status": "MODERATE",
  "currentSpeedKph": 28,
  "freeFlowSpeedKph": 50,
  "providerConfidence": 0.9,
  "functionalRoadClass": "FRC3",
  "fetchedAt": "2026-11-14T12:00:00Z",
  "expiresAt": "2026-11-14T15:00:00Z",
  "freshness": "FRESH",
  "attribution": "Traffic data © TomTom",
  "message": null,
  "pilotEnabled": true,
  "coverageAvailable": true,
  "coverageNote": "One nearby approach road segment only — not destination-wide coverage.",
  "interpretation": "PDCM interpretation: moderate congestion on the sampled approach road (28 vs 50 km/h free-flow)."
}
```

`status` may be `LIGHT` | `MODERATE` | `HEAVY` | `UNKNOWN` | `UNAVAILABLE`. Missing key, non-pilot place, budget exhaustion, rate-limit backoff, or provider errors yield `UNAVAILABLE` — never quiet. Distant/unsuitable geometry after zoom fallback yields `UNKNOWN`. Configure `PDCM_TOMTOM_API_KEY` on the API process only. Scheduled refresh stays **off** (`pdcm.external-traffic.schedule-enabled=false`). Hard budgets default to **500/day** and **15_000/month** Flow Segment requests (UTC); no Pay As You Grow.

### Crowd response (shape)

```json
{
  "destinationId": 1,
  "status": "BUSY",
  "score": 3.2,
  "confidence": "MEDIUM",
  "contributingReports": 3,
  "lastObservedAt": "2026-11-02T10:15:00Z",
  "asOf": "2026-11-02T10:30:00Z",
  "source": "USER_REPORTS"
}
```

When no usable reports: `status=UNKNOWN`, `score=null`, `confidence=NONE`, `contributingReports=0`.

Algorithms: [DATABASE_DESIGN.md](DATABASE_DESIGN.md).

### Traffic response (shape)

```json
{
  "destinationId": 1,
  "status": "MODERATE",
  "confidence": "LOW",
  "contributingReports": 2,
  "lastObservedAt": "2026-11-02T10:15:00Z",
  "asOf": "2026-11-02T10:30:00Z",
  "source": "USER_REPORTS"
}
```

`status` ∈ `LIGHT | MODERATE | HEAVY | UNKNOWN`. Separate from crowd scoring.

## Forecast

| Method | Path | Auth |
|---|---|---|
| GET | `/destinations/{id}/forecast` | public |

Query: `date` (optional ISO local date interpreted in `Asia/Kolkata`; default = tomorrow Kolkata).

### Always include calendar signals

```json
{
  "destinationId": 1,
  "targetDate": "2026-11-08",
  "calendarSignals": {
    "isWeekend": true,
    "isNationalHoliday": false,
    "isLongWeekend": false,
    "holidayName": null
  },
  "crowdEstimate": {
    "forecastStatus": "INSUFFICIENT_DATA",
    "estimatedIntensity": null,
    "estimatedStatus": null,
    "confidence": "NONE",
    "label": "ESTIMATE_UNAVAILABLE",
    "sampleSize": 2,
    "message": "Not enough same-weekday historical reports (need 5 in 90 days)."
  },
  "asOf": "2026-11-02T10:30:00Z"
}
```

**How `sampleSize` / `estimatedIntensity` are computed** (same rules as [DATABASE_DESIGN.md](DATABASE_DESIGN.md)):

1. Take reports for this destination with `observed_at` in the past 90 days whose Kolkata weekday matches `targetDate`.  
2. Dedup to latest per `(userId, India calendar date of observed_at)` — same-day multi-posts from one user do **not** inflate the count.  
3. If count < 5 → `INSUFFICIENT_DATA` as above.  
4. Else `estimatedIntensity = round(mean(intensities), 1)`, map to `estimatedStatus` via crowd bands; `forecastStatus=ESTIMATE`; confidence `LOW` (5–9) or `MEDIUM` (≥10); `label=ESTIMATE`. Never claim live observation.

**Implemented (Day 4).** At launch `sampleSize` is 0 until real same-weekday history exists. The submit API rejects `observedAt` older than 24 hours, so an `ESTIMATE` cannot be created through `POST /destinations/{id}/reports`. Calendar facts do not multiply the numeric estimate.

## Alternatives

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/destinations/{id}/alternatives` | public | **implemented** |

Query: `radiusKm` default **150**, max **500**; `limit` default **5**, max **20**. Unknown destination → 404.

```json
[
  {
    "destination": { "id": 2, "name": "City Palace", "slug": "city-palace" },
    "distanceKm": 0.4,
    "sameCategory": true,
    "crowdStatus": "UNKNOWN",
    "crowdScore": null,
    "crowdConfidence": "NONE",
    "informationQuality": "INSUFFICIENT_DATA",
    "contributingReports": 0,
    "lastObservedAt": null,
    "source": "USER_REPORTS"
  }
]
```

`informationQuality` is `SUFFICIENT` only when crowd confidence is not `NONE`. `UNKNOWN` stays `UNKNOWN`. `contributingReports` / `lastObservedAt` / `source` mirror live crowd for freshness display. Ranking: same category first, then a known crowd score before an unknown one, then lower score, then distance.

## Admin moderation (Phase 4)

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/admin/providers/traffic/usage` | ADMIN | TomTom Flow usage counters + remaining daily/monthly free-tier budget (no secrets) |
| GET | `/admin/reports?status=FLAGGED&limit=50` | ADMIN | review queue (`FLAGGED` or `PENDING`) |
| GET | `/admin/reports/{id}?includeCoordinates=false` | ADMIN | detail + audit history; no raw coords by default |
| POST | `/admin/reports/{id}/verify` | ADMIN | `FLAGGED`/`PENDING` → `LOCATION_VERIFIED` only if distance/coords within geofence |
| POST | `/admin/reports/{id}/reject` | ADMIN | `FLAGGED`/`LOCATION_VERIFIED` → `REJECTED` |
| POST | `/admin/reports/{id}/flag` | ADMIN | `LOCATION_VERIFIED` → `FLAGGED` (abuse) |

Body for actions (optional): `{ "note": "…" }`. Transitions write `report_moderation_events` (who, when, from→to, note). `LEGACY_UNVERIFIED` cannot be promoted via verify. Raw coords appear only when still retained **and** `includeCoordinates=true`; after purge they are absent even for admin. Never log retained coordinates. Public report/dashboard DTOs never include client lat/lon/accuracy.

## Destination suggestions (Phase 5)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/destination-suggestions` | USER | create suggestion; returns possible duplicate hints |
| GET | `/me/destination-suggestions` | USER | own submissions only |
| GET | `/admin/destination-suggestions?status=SUBMITTED&limit=50` | ADMIN | review queue (`SUBMITTED` or `REJECTED`) |
| GET | `/admin/destination-suggestions/{id}` | ADMIN | detail + duplicates + audit history |
| PATCH | `/admin/destination-suggestions/{id}/coordinates` | ADMIN | set/correct lat/lon on `SUBMITTED`; audits `COORDINATES_UPDATED`; refreshes duplicates |
| POST | `/admin/destination-suggestions/{id}/approve` | ADMIN | creates `destinations` row; conflict if already decided / missing coords |
| POST | `/admin/destination-suggestions/{id}/reject` | ADMIN | reject with optional note |

Create body:

```json
{
  "name": "Quiet Hill Overlook",
  "city": "Mussoorie",
  "region": "Uttarakhand",
  "category": "HILL_STATION",
  "description": "Lesser-known ridge viewpoint east of the mall.",
  "latitude": 30.4562,
  "longitude": 78.0781,
  "referenceUrl": "https://example.com/ref"
}
```

`latitude`/`longitude` are optional as a pair; soft India bbox applies when present. `referenceUrl` must be http(s) when provided. Categories are the existing enum only (`HISTORICAL`, `TEMPLE`, `BEACH`, `HILL_STATION`, `WILDLIFE`, `CITY`, `NATURE`).

Admin coordinate update body (both required):

```json
{
  "latitude": 28.541,
  "longitude": 77.098
}
```

WGS84 ranges apply (`latitude` −90…90, `longitude` −180…180); null, out-of-range, null-island, and soft-India-box failures return 400. Only `SUBMITTED` suggestions may be updated (409 otherwise). A successful update appends a `COORDINATES_UPDATED` audit event (admin actor + note with the new values) and returns detail with refreshed duplicate hints. Approve still requires valid coordinates.

Owner responses never include email; `contributorUserId` is admin-only. Approve does **not** create visit reports; new destinations start with UNKNOWN crowd/traffic. Users cannot call approve/reject/coordinates (403). Visit-report GPS is never exposed via these endpoints.

## Dashboard

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/dashboard` | public | **implemented** |

```json
{
  "asOf": "2026-11-06T12:00:00Z",
  "observationWindowHours": 6,
  "catalogueCoverage": {
    "activeDestinationCount": 100,
    "regionCount": 12,
    "regions": ["Delhi", "Rajasthan"]
  },
  "externalTrafficProvider": {
    "provider": "TOMTOM",
    "status": "DISABLED",
    "configured": false,
    "scheduleEnabled": false,
    "pilotSlugs": ["hawa-mahal"]
  },
  "popularDestinations": [
    {
      "destination": { "id": 1, "name": "Hawa Mahal" },
      "crowd": {
        "status": "UNKNOWN",
        "confidence": "NONE",
        "score": null,
        "contributingReports": 0,
        "lastObservedAt": null,
        "source": "USER_REPORTS"
      },
      "traffic": {
        "status": "UNKNOWN",
        "confidence": "NONE",
        "source": "USER_REPORTS"
      },
      "recentReportCount": 0,
      "verifiedRecentReportCount": 0,
      "forecastQuality": {
        "targetDate": "2026-11-07",
        "forecastStatus": "INSUFFICIENT_DATA",
        "estimatedIntensity": null,
        "estimatedStatus": null,
        "confidence": "NONE",
        "sampleSize": 0,
        "message": "Not enough same-weekday historical reports (need 5 in 90 days)."
      }
    }
  ],
  "calendarHighlight": {
    "targetDate": "2026-12-25",
    "label": "LONG_WEEKEND",
    "calendarSignals": {
      "isWeekend": false,
      "isNationalHoliday": true,
      "isLongWeekend": true,
      "holidayName": "Christmas"
    }
  },
  "recentReports": [],
  "quieterKnownDestinations": []
}
```

Popular destinations are the six active places with the most reports in the last 7 days (name breaks ties) — **activity/popularity**, not verified crowd. `verifiedRecentReportCount` counts `LOCATION_VERIFIED` reports in that week; live crowd/traffic badges still use only the last **6 hours** of `LOCATION_VERIFIED` reports. With no reports the list is still six places and crowd/traffic are `UNKNOWN`. `forecastQuality` is tomorrow’s same-weekday historical estimate (or `INSUFFICIENT_DATA` with null intensity) — never a live observation; calendar stays on `calendarHighlight`. `observationWindowHours` mirrors `ReportWindow.FRESHNESS` (6). `catalogueCoverage` is catalogue size/regions only. `externalTrafficProvider` is **display-only** provider health from the existing status path — the dashboard does **not** call TomTom Flow. Crowd (attraction), community visitor traffic (`traffic.source=USER_REPORTS`), and external road traffic remain separate channels. The calendar highlight is the next long weekend within 60 India days, otherwise the next Saturday. Recent reports are the latest eight, omit email/display name, and include public-safe `trustLabel`. `quieterKnownDestinations` contains only places whose crowd confidence is not `NONE`, lowest score first, at most three. An empty list means no known crowd, not that everywhere is quiet.

Alternatives (`GET /destinations/{id}/alternatives`) also expose `contributingReports`, `lastObservedAt`, and `source` from live crowd so clients can show freshness without treating `UNKNOWN` / `INSUFFICIENT_DATA` as quiet.

**Long weekend:** Friday through Monday when that Friday or that Monday is a national holiday. A midweek holiday is a holiday only. Seeded holidays are fixed-date national days for 2026–2027 (Republic Day, Independence Day, Gandhi Jayanti, Christmas). Lunar and state holidays are not invented.

## Error shape

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Human-readable summary",
  "details": []
}
```

Common codes: `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `REPORT_RATE_LIMITED`, `CONFLICT`.
