# Technical Decisions

## Stack

| Decision | Choice | Why |
|---|---|---|
| Language/runtime | Java 25 | Target LTS line; Boot 4.1 supports 17–26 |
| Framework | Spring Boot **4.1.x** | Current 4.x; first-class Java 25; use Boot 4 starter names (`webmvc`, `security-oauth2-resource-server`) |
| Build | Maven, single module | One deployable for 7-day MVP |
| Persistence | PostgreSQL 16 + Flyway + JPA | Interview-credible schema history; `ddl-auto=validate` |
| Auth | Spring Security 7 + Nimbus JWT (self-issued) | No Keycloak; no hand-rolled jjwt filter; lambda DSL only |
| API docs | Markdown contracts in `docs/API_DOCUMENTATION.md` | springdoc/Swagger UI not wired yet |
| Frontend | React + TS + Vite + Tailwind | Fast dashboard; low ceremony |
| Geo | lat/lon + Haversine | PostGIS unnecessary for MVP radius ranking |
| Cache/queue | None in P0 | Redis/Kafka are P1/P2 |

## Data & API

| Decision | Choice |
|---|---|
| Tags | `@ElementCollection` → `destination_tags(destination_id, tag)` |
| Holiday | Standalone table; no FKs |
| Visit report | Single entity for crowd + traffic |
| Scores | Computed on read; no score tables |
| Timestamps | UTC storage; Kolkata for India day/holidays/caps; API `…Z` |
| JDBC TZ | Force session `TimeZone=UTC` via Hikari init SQL + test `-Duser.timezone=UTC` |
| Postgres host port | **5433→5432** so Docker does not collide with local Windows Postgres on 5432 |
| Pagination JSON | Stable `PageResponse` DTO (not raw Spring `Page` serialization) |
| Nearby radius | Max **500 km** |
| JWT | Self-issued HS256 via Nimbus `JwtEncoder`/`JwtDecoder` + oauth2ResourceServer |
| Refresh tokens | Deferred (P1) |

## Crowd algorithm

Weights, thresholds, agreement (`spread ≤ 1` / `≥ 2`), and confidence matrix are fixed in [DATABASE_DESIGN.md](DATABASE_DESIGN.md). Keep deterministic and unit-tested. No percentage confidence.

## Traffic algorithm

Independent of crowd: mode of non-`UNKNOWN` traffic in the same 6h latest-per-user set; severity wins ties. Own confidence-by-count. Separate unit tests.

## Forecast honesty & formula

- Calendar signals always.  
- Dedup: latest report per `(user, India calendar day)` among same-weekday reports in 90 days — prevents one user from inflating `sampleSize` with many same-day posts.  
- Numeric estimate iff deduped `sampleSize ≥ 5`.  
- `estimatedIntensity` = mean of those intensities (1 decimal); status bands identical to current crowd.  
- Else `INSUFFICIENT_DATA` with null numerics.  
- No fabricated history. Weather = P1.
- Long weekend = Friday–Monday span whose Friday or Monday is a national holiday. Midweek holidays stay holidays only.
- Holiday seed is fixed-date national days for 2026–2027. The submit API’s 24-hour backdate means production cannot insert the 90-day history an `ESTIMATE` needs; tests insert those rows through the repository.

## `observedAt` vs `created_at`

- Client may set `observedAt` within **[now − 24h, now + 60s]**. Outside → 400.  
- **Cooldown and daily cap use `created_at` only**, so backdating cannot bypass rate limits.  
- During crowd aggregation, effective age is `max(0, now − observed_at)` so a permitted future skew (≤60s) counts as **age zero** / full weight — never a negative age.

## Report concurrency (no Redis)

**Problem:** concurrent POSTs can bypass cooldown/daily cap under check-then-act.

**Solution:**

1. `@Transactional` submit.  
2. Acquire **exactly one** `pg_advisory_xact_lock` keyed by **`user_id` only** (e.g. `hashtext('pdcm:report:' || user_id)`).  
3. Under that lock: enforce daily cap (10 / India day, all destinations) and per-destination 30‑min cooldown; then INSERT.  
4. Lock released at commit/rollback.

**Why user-scoped single lock:** serializes concurrent submits to **different destinations** so the global daily counter is safe; also serializes same-destination doubles. One lock key per transaction → no multi-lock deadlock cycles.

Integration test: parallel submits from one user must not exceed caps.

## Security (MVP)

BCrypt · short-lived JWT · role checks · Bean Validation · generic login errors · CORS to Vite origin · secrets via env · note length cap · no email on public reports · admin bootstrap password from env.

## Catalogue expansion & map discover

- Destination names/coordinates for new rows: Wikidata structured data (**CC0**), manually verified; original descriptions; provenance columns on `destinations` (Flyway **V6** only — never edit applied migrations).
- Legacy Day-1 rows keep `LEGACY_SEED` / `UNKNOWN` license rather than inventing a source.
- Map library: **Leaflet + React-Leaflet** (compatible with React 19). OSM standard raster tiles only; configurable `VITE_OSM_TILE_URL`; visible OSM attribution; no paid map/geocode, no OSM import into the catalogue.
- Map API: bounding-box query with shared filters, max span 40°, default limit 100 / max 200. Crowd/traffic stay on existing per-destination endpoints and are requested only for the selected marker/list item.

## External traffic (TomTom) — on-demand + free-tier budgets

- On-demand Flow only; shared per-destination **in-memory** cache (default 3h); `schedule-enabled=false`.
- Budget consume is **DB-atomic**: `pg_advisory_xact_lock` + conditional `UPDATE` on `provider_request_stats` so concurrent API instances cannot exceed daily/monthly caps (UTC day/month).
- Zoom fallback attempts each consume budget. Stale in-memory observations may be returned when budget is exhausted.
- TomTom client logs sanitize URLs and `key=` values; exception messages never include the request URI.
- Licensing: see `docs/TOMTOM_LICENSING_NOTES.md` — public Flow terms mention ≤24h server storage / no historical warehouse; **not** a project-specific approval.

## External traffic pilot (TomTom) — historical note + expansion posture

- Provider-neutral `TrafficProvider`; TomTom Flow Segment Data for **slug allowlist only** (never nationwide).
- **Configured pilots today:** the original five (`amber-fort`, `gateway-of-india`, `hawa-mahal`, `india-gate`, `taj-mahal`). A ≥50 expansion was prepared with 60 catalogue candidates, but **live geometry validation could not run** without `PDCM_TOMTOM_API_KEY` (0 Flow requests made). Do not treat unvalidated candidates as supported. See [TOMTOM_PILOT_COVERAGE.md](TOMTOM_PILOT_COVERAGE.md) and `scripts/Validate-TomTomPilotCoverage.ps1`.
- **On-demand only** with a **3-hour** (configurable) shared destination **in-memory** cache. No background polling; `schedule-enabled=false`.
- Single-flight per destination; zoom fallback **18 → 15 → 12 → 10** (each TomTom hit counts); reject distant geometry (>750 m).
- One representative road sample per destination — not destination-wide coverage (UI: Supported / Unavailable / Stale).
- Postgres stores `provider_health` + daily request stats (V7) — **not** TomTom payloads. Monthly usage is summed from daily rows (no new Flyway). Licence review still blocks persistent traffic payload storage.
- Key via `PDCM_TOMTOM_API_KEY` on the API only; never in React/Git. Map tiles remain OSM.
- Hard budgets: **500/day** and **15_000/month** Flow (UTC); ADMIN `GET /api/admin/providers/traffic/usage` exposes remaining budget. No Pay As You Grow / silent paid switch.
- **Budget math for a hypothetical verified 50-slug pilot:** 50×8 TTL refreshes/day = 400/day ≈ 12k/month at 1 call each (~3k headroom under 15k). Worst-case 4 zooms → 1600/day / 48k/month exceeds free-tier and app caps; hard budgets always win; no promise of 8 refreshes/day for every place.
- Community `/traffic` and external `/traffic/external` are separate channels; UI shows both and never merges them. TomTom never drives crowd intensity.
- Traffic Incidents remain disabled (would be capped at 1_500/month if ever enabled).


## GPS community verification (Phase 3)

- Statuses: `LEGACY_UNVERIFIED` (historical backfill), `PENDING`, `LOCATION_VERIFIED`, `FLAGGED`, `REJECTED`.
- After cutover, crowd/traffic/forecast history use **only** `LOCATION_VERIFIED`.
- GPS is an optional consent-gated proximity check (default 500 m / 150 m accuracy); not absolute proof.
- Precise client coordinates are private (stored server-side for audit; never on public APIs).
- Permission denial / unavailable GPS / coarse accuracy → accept report as `PENDING` without blocking submit.
- Soft spoofing gate (null island / outside rough India box) → `FLAGGED`, still rate-limited like other submits.
- TomTom schedule and Traffic Incidents remain disabled.

## Trust / moderation (Phase 4)

- Keep geofence **500 m** and accuracy **150 m** thresholds configurable; do not silently change them.
- Raw client lat/lon/accuracy purged after **max 24h** retention by default (`pdcm.report-verification.coordinate-retention: PT24H`); job is idempotent and restart-safe. After purge: keep status, reason, distance, consent; set `location_purged_at`. Never log raw coords.
- ADMIN-only `/api/admin/reports` queue: list FLAGGED/PENDING, detail, verify / reject / flag with audit events.
- `FLAGGED`/`PENDING` → `LOCATION_VERIFIED` only via explicit verify that confirms location eligibility (distance within geofence and/or retained coords). **Never** promote `LEGACY_UNVERIFIED` this way. Moderation never silently promotes unverified reports into aggregation.
- `LOCATION_VERIFIED` → `REJECTED` or `FLAGGED` for abuse; rejected/flagged drop out of aggregation immediately.
- Dashboard: `recentReportCount` = activity/popularity; `verifiedRecentReportCount` = LOCATION_VERIFIED week count; live crowd badges remain 6h LOCATION_VERIFIED only. Public `trustLabel` explains quality without email/PII.
- Admin may request retained coords with `includeCoordinates=true` only while retained; after purge they are absent even for admin. Prefer distance/status for routine review.

## Destination suggestions (Phase 5)

- Separate `destination_suggestions` table/workflow — not an extension of `VisitReport`.
- Statuses: `SUBMITTED` → `APPROVED` | `REJECTED` (NEEDS_INFO deferred).
- Duplicate hints: normalized name + region + Haversine proximity (≤2 km) against active catalogue; submit still allowed.
- Soft India bbox validation when coords provided; both lat/lon required together; approval requires coordinates (catalogue map needs them).
- Approve uses `pg_advisory_xact_lock` per suggestion id + status check to prevent double-approve races; unique partial index on `created_destination_id`.
- Created destinations: `source_reference=USER_SUGGESTION`, `source_license=UNKNOWN`, unique slug; crowd/traffic remain UNKNOWN with **zero** synthetic visit reports.
- Public/owner APIs never expose contributor email; admin may see `contributorUserId` only (same privacy posture as report moderation).
- Reuse existing `DestinationCategory` enum only — no CULTURAL/ADVENTURE expansion in this phase.
- TomTom schedule remains off; Traffic Incidents untouched; no paid geo/AI APIs.

## Dashboard information quality (Phase 6)

- Dashboard reuses `CrowdAggregator` / `TrafficAggregator` / `ForecastCalculator` / `ExternalTrafficService.providerStatus()` — no duplicate scoring, no Flow calls from `/dashboard`.
- Additive API fields only; no Flyway. Live crowd/traffic remain 6h LOCATION_VERIFIED; forecast quality is tomorrow’s estimate or `INSUFFICIENT_DATA`.
- UI never merges attraction crowd, community visitor traffic, and external road provider; DISABLED/unconfigured provider ≠ clear roads; UNKNOWN ≠ quiet.

## Explicitly deferred

Redis rate limiting, Open-Meteo, LLM insights, CI/CD, AWS, RAG, WebSockets, microservices, CULTURAL/ADVENTURE categories, NEEDS_INFO suggestion status / request-info API, weighted trust scoring beyond status labels, TomTom schedule enablement, Traffic Incidents, pilot evaluation rollout.
