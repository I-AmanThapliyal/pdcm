# Project Requirements

## Problem

Help travelers decide whether a tourist destination is worth visiting **now** or later, using community reports plus transparent estimates — without pretending to have live sensor counts.

## Honesty rules (non-negotiable)

- Never present estimates as verified live observations.
- **No reports** → status `UNKNOWN` (not quiet).
- **Insufficient historical data** → forecast `INSUFFICIENT_DATA` (no numeric crowd forecast).
- Do not seed or fabricate historical reports.
- Show **freshness** and **confidence** with every crowd/traffic summary.
- Distinguish: user observations · calendar facts · rule-based estimates.

## Roles

| Role | Capabilities |
|---|---|
| Anonymous | Browse destinations, crowd/traffic summaries, forecasts, alternatives, dashboard |
| USER | Register/login, submit visit reports, view profile |
| ADMIN | Create/update/deactivate destinations |

## P0 — MVP (7 days)

1. Spring Boot API + PostgreSQL + Flyway  
2. Destination browse/search/filter/detail/nearby  
3. Register, login, JWT, USER/ADMIN  
4. Visit reports (crowd + traffic in one submission)  
5. Crowd aggregation with documented weights/thresholds  
6. Traffic aggregation (separate algorithm)  
7. Report freshness, 30‑min cooldown, daily cap (10/user/India day)  
8. Forecast: calendar signals always; numeric estimate only if enough history  
9. Alternative destinations (never “quiet” when data missing)  
10. React dashboard wired to API  
11. Essential unit + integration tests  
12. Docker Compose  
13. GitHub + professional README  

## P1 — after MVP stable

Redis caching · scheduled jobs · GitHub Actions · cloud deploy (cost-approved) · Open-Meteo weather · optional LLM explanation that quotes rule-based results only

## P2 — later

RAG/vectors · ML forecasting · WebSockets · Kafka · K8s · microservices · PostGIS · live traffic providers

## Domain rules (summary)

Full algorithms: [DATABASE_DESIGN.md](DATABASE_DESIGN.md). Decisions: [TECHNICAL_DECISIONS.md](TECHNICAL_DECISIONS.md).

| Topic | Rule |
|---|---|
| Report window (current) | Latest report per user per destination within 6 hours |
| Daily report cap | 10 per user per India calendar day (`Asia/Kolkata`) |
| Cooldown | 1 report per user per destination per 30 minutes |
| Forecast numeric | ≥ 5 **deduped** same-weekday observations in 90 days (latest per user per India day); mean → `estimatedIntensity`; else `INSUFFICIENT_DATA` |
| `observedAt` | Reject future (>60s skew) and older than 24h; cooldown/daily cap use `created_at` |
| Timestamps | Store UTC; India day/holidays in Kolkata; API ISO-8601 `Z` |

## Definition of done

Matches README status checklist: register/login, destinations, reports, crowd + traffic + freshness + confidence, honest forecasts, alternatives, dashboard, docs, tests run, Compose, GitHub README. Cloud/Redis/AI are not blockers.
