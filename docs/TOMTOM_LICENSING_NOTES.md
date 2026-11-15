# TomTom licensing & caching notes (PDCM)

**Status:** PDCM has **not** obtained a separate legal opinion or TomTom account-specific waiver. This note records what public TomTom product terms say and how PDCM behaves. **Do not treat this as licensing approval.**

## Public product-term evidence (reviewed 2026-11-15)

TomTom’s published *Third Party Product Terms / EULA* materials include language for **TomTom Flow** (and similarly TomTom Traffic) along these lines:

1. Do **not** store the data for more than **twenty-four (24) hours** on your servers.  
2. Do **not** broadcast / make the product available except to authorized end users.  
3. Do **not** use the feed for **historical** data purposes (collection or analysis).

Sources consulted (public PDFs / legal pages — verify against the exact Terms accepted with your API key):

- [TomTom Third Party Product Terms (2017 banner PDF)](https://download.tomtom.com/open/banners/third-party-product-terms-eula-2017-06.pdf) — table rows for “TomTom Flow” / “TomTom Traffic” include the 24-hour storage and no-historical-use clauses.  
- [TomTom Third Party Product Terms overview](https://www.tomtom.com/en-us/legal/third-party-product-terms/) and current EULA PDFs may supersede older banners — **confirm the package that binds your developer account**.

Traffic-related products also restrict use for traffic-law enforcement (e.g. speed-camera placement). PDCM does not do that.

## What PDCM does today (engineering choice, not a legal clearance)

| Topic | PDCM behaviour |
|---|---|
| Flow payloads | **In-memory only**, default TTL **3 hours** (`cache-ttl-minutes: 180`) — well under a 24-hour upper bound from public Flow terms, **if** those terms apply to your account |
| Postgres | **Metadata only** (`provider_health`, daily `provider_request_stats` via V7) — never TomTom response bodies |
| Historical warehouse | **Not built** — no long-lived Flow history tables |
| Map tiles | OSM/Leaflet — **not** TomTom tiles |
| Incidents | Disabled |
| Scheduled refresh | Disabled (`schedule-enabled=false`) |

Serving a **stale** in-memory observation briefly when the free-tier budget is exhausted still keeps that payload only in process memory (not Postgres). TTL remains 3 hours for successful FRESH entries; failed/unavailable placeholders use a short TTL.

## Explicit non-claims

- PDCM does **not** claim that TomTom has approved this project.  
- PDCM does **not** claim that free-tier portal Terms are identical to the 2017 EULA table text.  
- Before production live traffic, an operator should **read the Terms shown when the API key was created** and keep a copy. If those Terms forbid even short-lived caching, disable the provider (`PDCM_TOMTOM_API_KEY` unset / `enabled=false`) until clarified.

## If Terms remain unclear or stricter

Keep in-memory cache only (current behaviour). Do **not** add Flyway tables for traffic observations. Prefer serving `UNAVAILABLE` over persisting restricted payloads.
