# Destination data provenance

## Catalogue size

- **20** legacy Day-1 seeds (`source_reference = LEGACY_SEED`, `source_license = UNKNOWN`, `source_url` null).
- **80** verified Wikidata additions (`source_reference = Wikidata Q…`, `source_url` to the entity, `source_license = CC0`).
- **100** active destinations total after Flyway **V6**.

No `visit_reports`, crowd levels, traffic conditions, or forecasts are seeded. Places without real reports continue to return **UNKNOWN**.

## Licensing

| Source | What we use | License |
|---|---|---|
| [Wikidata structured data](https://www.wikidata.org/wiki/Wikidata:Licensing) | Candidate names and coordinates (`P625`) | **CC0** |
| Official / encyclopedia pages | Factual cross-check only | Descriptions are **original PDCM wording** — source prose is not copied |
| OpenStreetMap | Interactive map **tiles only** (not destination catalogue rows) | © OpenStreetMap contributors; see [copyright](https://www.openstreetmap.org/copyright) and [tile usage policy](https://operations.osmfoundation.org/policies/tiles/) |

OSM vector/map features are **not** imported into `destinations`.

## Review method

1. Build a candidate list of notable Indian tourist places with Wikidata Q-ids.
2. Resolve each entity via Wikidata (`wbgetentities` / entity page) and read `P625` coordinates.
3. Keep only points that fall inside a generous India bounding box (approx. lat 6–36, lon 68–98).
4. Reject wrong Q-ids, non-India coordinates, or identity that cannot be reasonably confirmed.
5. Skip candidates that already exist as Day-1 slugs (e.g. Taj Mahal, Hawa Mahal, Hampi, Shimla, Manali, Darjeeling, Madurai Meenakshi, Golden Temple).
6. Write short original descriptions; store city, region, category, and provenance columns.
7. Record verified candidates in `docs/data/wikidata-verified-candidates.csv`. Regeneration helper: `scripts/generate-v6-seed.ps1`.

## Omitted candidates (examples)

Left out when identity/coordinates could not be verified, SPARQL/API resolution failed, the Q-id pointed at the wrong place, or the place already existed in V2:

- Wrong or ambiguous Wikidata matches encountered during lookup (discarded rather than guessed).
- Overlaps with existing V2 seeds (Golden Temple, Hampi, Shimla, Manali, Darjeeling, Madurai, and other Day-1 places).
- Candidates outside the India coordinate gate or lacking usable `P625`.

## Map tiles

Frontend default tile URL: `https://tile.openstreetmap.org/{z}/{x}/{y}.png` (override with `VITE_OSM_TILE_URL`). Community tiles are **best-effort** with **no availability guarantee**. The UI links attribution to https://www.openstreetmap.org/copyright and does not prefetch, proxy, scrape, or offline-cache tiles beyond normal browser behavior.
