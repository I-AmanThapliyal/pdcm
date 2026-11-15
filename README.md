# Please Don't Come Here (PDCM)

Intelligent tourist crowd and traffic advisory platform. Travelers see **current crowd status**, **traffic**, **freshness**, **confidence**, and **honest forecasts** before visiting a destination.

No reports means **unknown**, not quiet. Insufficient history means **insufficient data**, not a fabricated score.

## Status

**Day 5 complete, plus catalogue/map expansion:** the React app at `frontend/` covers the dashboard, destination search, **map discover** (Leaflet + OSM tiles), place detail (crowd, traffic, forecast, alternatives), registration, and visit reports. Catalogue is **~100 verified India destinations** with city + provenance fields. A missing crowd stays unknown. A forecast without five same-weekday reports stays insufficient data.

## Stack (approved)

| Layer | Choice |
|---|---|
| Backend | Java 25, Spring Boot 4.1.x, Maven, Spring Security 7 + Nimbus JWT |
| Database | PostgreSQL 16, Flyway, JPA/Hibernate (`ddl-auto=validate`) |
| Frontend | React, TypeScript, Vite, Tailwind |
| Docs API | springdoc-openapi 3.2.x |
| Tests | JUnit 5, Mockito, Spring Boot Test, Testcontainers |
| Run | Docker Compose (local) |

## MVP geography

India · timezone `Asia/Kolkata` · national holidays · **~100** seeded destinations (20 legacy + 80 Wikidata-verified). Provenance: [docs/DATA_PROVENANCE.md](docs/DATA_PROVENANCE.md).

## Documentation

| Doc | Purpose |
|---|---|
| [docs/PROJECT_REQUIREMENTS.md](docs/PROJECT_REQUIREMENTS.md) | Product rules, P0/P1/P2 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Modules, request flow |
| [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) | Schema, crowd/traffic algorithms |
| [docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md) | Endpoint contracts |
| [docs/DEVELOPMENT_ROADMAP.md](docs/DEVELOPMENT_ROADMAP.md) | 7-day plan |
| [docs/TECHNICAL_DECISIONS.md](docs/TECHNICAL_DECISIONS.md) | Decision log |
| [docs/LEARNING_NOTES.md](docs/LEARNING_NOTES.md) | Interview checklist |
| [docs/DATA_PROVENANCE.md](docs/DATA_PROVENANCE.md) | Destination sourcing, licensing, map attribution |
| [docs/proposals/PHASE0_HYBRID_TRAVEL_INTELLIGENCE.md](docs/proposals/PHASE0_HYBRID_TRAVEL_INTELLIGENCE.md) | **Proposed** hybrid architecture (awaiting approval) |

## Local run

Requires Java 25, Docker, and Maven Wrapper (`backend/`).

```bash
# Terminal 1 — database (repo root)
docker compose up -d
# Postgres on localhost:5433

# Terminal 2 — API
cd backend
# Optional: set PDCM_JWT_SECRET to a 32+ char secret
# Optional admin bootstrap: set PDCM_ADMIN_PASSWORD
set JAVA_TOOL_OPTIONS=-Duser.timezone=UTC
mvnw.cmd spring-boot:run
```

```bash
cd backend
mvnw.cmd test
```

```bash
# Terminal 3 — UI (Node.js 24). Vite proxies /api to the API.
cd frontend
npm install
npm run dev
# http://localhost:5173 — Map discover at /map
```

Optional: set `VITE_OSM_TILE_URL` to change the OSM raster tile template (default `https://tile.openstreetmap.org/{z}/{x}/{y}.png`). Community tiles are best-effort with no availability guarantee. See [tile usage policy](https://operations.osmfoundation.org/policies/tiles/).

Optional TomTom pilot (API process only — never in Vite/React):

```bash
set PDCM_TOMTOM_API_KEY=your_free_tier_key
# On-demand only — keep schedule off (default):
# pdcm.external-traffic.schedule-enabled=false
# Shared destination cache TTL (default 180 minutes):
# pdcm.external-traffic.cache-ttl-minutes=180
# Free-tier hard caps (UTC): daily 500, monthly Flow 15000
```

External road traffic stays separate from community traffic. One nearby road sample per destination (not destination-wide). Paid overages / Pay As You Grow stay disabled via hard daily + monthly request budgets. ADMIN: `GET /api/admin/providers/traffic/usage`.

Useful checks:

```bash
curl "http://localhost:8080/api/destinations"
curl "http://localhost:8080/api/destinations/map?south=6.5&west=68&north=35.5&east=97&limit=100"
curl -X POST "http://localhost:8080/api/auth/register" -H "Content-Type: application/json" -d "{\"email\":\"you@example.com\",\"displayName\":\"You\",\"password\":\"password123\"}"
curl -X POST "http://localhost:8080/api/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"you@example.com\",\"password\":\"password123\"}"
curl "http://localhost:8080/api/me" -H "Authorization: Bearer <token>"
curl -X POST "http://localhost:8080/api/destinations/1/reports" -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"crowdIntensity\":3,\"trafficCondition\":\"MODERATE\",\"observedAt\":\"2026-11-14T12:00:00Z\",\"locationConsent\":false}"
curl "http://localhost:8080/api/destinations/1/crowd"
curl "http://localhost:8080/api/destinations/1/traffic"
curl "http://localhost:8080/api/destinations/1/traffic/external"
curl "http://localhost:8080/api/providers/traffic/status"
curl "http://localhost:8080/api/destinations/1/forecast?date=2026-12-25"
curl "http://localhost:8080/api/destinations/1/alternatives?radiusKm=20"
curl "http://localhost:8080/api/dashboard"
```

The UI is `http://localhost:5173` (map at `/map`; suggest at `/suggest`; ADMIN moderation at `/admin/moderation`; ADMIN suggestions at `/admin/suggestions`). Swagger is still later. Flyway **V5** adds `holidays` and the forecast history index. Flyway **V6** adds `city` / provenance columns and ~80 verified destinations (no fabricated reports). Flyway **V8** adds GPS verification columns; **V9** adds moderation audit + coord-purge support (default 24h retention). Flyway **V10** adds destination suggestions + approval audit (approved places join the catalogue with UNKNOWN crowd/traffic; no synthetic reports).

## License

Personal portfolio project. Not for commercial use of third-party free APIs without checking their terms.
