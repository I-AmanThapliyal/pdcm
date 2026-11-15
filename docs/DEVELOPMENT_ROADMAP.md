# Development Roadmap

Constraint: **7 calendar days**, ~**25–30 hours**. Cut P1/P2 before cutting P0.

## Day plan

| Day | Focus | Exit criteria |
|---|---|---|
| 1 | Maven app, Flyway, Compose Postgres, destination reads, seed data | **Done** — public list/detail/search/nearby + tests green |
| 2 | Register/login/JWT/roles, visit reports, advisory-lock limits | **Done** — auth + reports + concurrency-safe caps (22 tests) |
| 3 | Crowd + traffic aggregation, freshness, unit tests | **Done** — public crowd/traffic GETs; 35 tests green |
| 4 | Forecast (honest), alternatives, dashboard API | **Done** — `INSUFFICIENT_DATA` + calendar signals; alternatives never call missing data quiet; public dashboard; 44 tests green |
| 5 | React UI + API integration | **Done** — Vite app: dashboard, search, detail, forecast, alternatives, register, visit report |
| 5b | Catalogue + map discover | **Done** — ~100 destinations (V6 city/provenance), `GET /destinations/map`, Leaflet/OSM map+list UI, provenance docs, tests |
| 6 | Testcontainers, security/validation tests, full Compose, bugfix | Critical tests green |
| 7 | README polish, GitHub, interview prep; optional CI/AI only if stable | Portfolio-ready MVP |

## Hybrid travel intelligence (proposed — awaiting approval)

Phase 0 architecture proposal (no production code yet):

**[docs/proposals/PHASE0_HYBRID_TRAVEL_INTELLIGENCE.md](proposals/PHASE0_HYBRID_TRAVEL_INTELLIGENCE.md)**

Approved sequence: (1) map loading fix → (2) TomTom traffic pilot → (3) GPS verification → (4) trust/moderation → (5) destination suggestions → (6) dashboard information quality → (7) pilot evaluation. Implement one phase at a time.

**Phase 1 (map loading fix): done** — preserve list during refresh; suppress bounds refetch on selection pan.

**Phase 2 (TomTom traffic pilot): done / revised** — `externaltraffic` package; on-demand Flow with **3h** in-memory shared cache; single-flight; zoom fallback 18→15→12→10; geometry validation; daily+monthly free-tier hard budgets (15k Flow/month); provider health/stats (V7); ADMIN usage endpoint; dual traffic UI with honest single-road coverage; schedule remains **off**; Incidents not implemented. Set `PDCM_TOMTOM_API_KEY` locally to exercise live Flow calls.

**Phase 3 (GPS community verification): done** — V8 verification columns; geofence proximity; `LOCATION_VERIFIED`-only aggregation cutover; consent UX; precise coords never public.

**Phase 4 (trust/moderation): done** — V9 moderation audit; 24h configurable coord purge; ADMIN verify/reject/flag; dashboard popularity vs verified crowd honesty; public trust labels without PII.

**Phase 5 (destination suggestions): done** — V10 `destination_suggestions` + audit events; USER submit/list-own; ADMIN review approve/reject with duplicate hints; approval creates catalogue destination (`USER_SUGGESTION` provenance) with UNKNOWN crowd/traffic and **no** synthetic visit reports. Concurrent approve guarded by advisory lock.

**Phase 6 (dashboard information quality): done** — additive dashboard fields (`observationWindowHours`, `catalogueCoverage`, `externalTrafficProvider` display-only, per-destination `forecastQuality`); batch fresh/history queries (no N+1); UI separates crowd vs community traffic vs external provider, freshness/confidence/source, insufficient forecast, UNKNOWN≠quiet; alternatives expose freshness fields. Next (awaits approval): Phase 7 pilot evaluation — not CULTURAL/ADVENTURE, TomTom schedule, or Incidents.

## Cut line

If behind: drop Day 7 optionals first, then thin UI polish — **never** drop auth, reports, crowd honesty, Compose, or core tests.

## Phase gates

| Phase | Content | Gate |
|---|---|---|
| 0 | Architecture + docs | Done when docs match approved rules |
| 1 | Day 1 backend foundation | Explicit user approval |
| … | Days 2–7 | Day-end check-ins |

## Day-end report template

- Completed  
- Works  
- Tests run + results  
- Remaining  
- On schedule?  
- Next highest-priority task  
