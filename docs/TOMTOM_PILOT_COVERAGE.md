# TomTom pilot coverage validation

Generated (UTC): 2026-11-16T19:54:33Z

## Method

- Candidates: existing catalogue slugs only (see `docs/data/tomtom-pilot-candidates.csv`).
- Priority: Delhi NCR first, then famous Indian tourist sites; original five retained.
- Zoom fallback: 18 → 15 → 12 → 10 (same as `pdcm.external-traffic.flow-zoom-fallbacks`).
- Geometry: nearest Flow coordinate must be ≤ **750 m** (`FlowSegmentGeometryValidator` / `max-segment-distance-meters`).
- HTTP success alone is **not** usable coverage.
- Caps: ≤4 Flow requests/candidate, ≤200 total for this exercise.
- API key: read from `PDCM_TOMTOM_API_KEY` only; never logged or written to these files.

## Summary

| Metric | Value |
|---|---|
| Candidates evaluated | 60 |
| Usable coverage (verified) | 52 |
| Not validated (key missing / skipped) | 0 |
| Failed / unusable | 8 |
| Flow requests used | 95 |

## Budget mathematics (product, not this one-off)

| Assumption | Math |
|---|---|
| Shared cache TTL | 180 minutes → at most ~8 theoretical refreshes/destination/day if every TTL is exhausted |
| 50 pilots × 8 | 400 refreshes/day |
| If each refresh is 1 Flow call | 400 × 30 ≈ **12_000**/month |
| App monthly hard cap | **15_000** → ~3_000 headroom for zoom fallbacks |
| Worst case 4 zooms every refresh | 1_600/day → **48_000**/month — exceeds free-tier and app cap |
| Daily hard budget | **500** always wins over desired refresh cadence |
| TomTom free monthly | ~20_000; PDCM stays under 15_000 Flow and never enables Pay As You Grow |

Schedule refresh and Traffic Incidents stay **off**. No promise of 8 refreshes/day for every pilot. On-demand + shared 3h cache only.

## Results table

| Name | Slug | Coverage outcome | Nearest m | Success zoom | Requests |
|---|---|---|---|---|---|
| India Gate | `india-gate` | USABLE | 175 | 18 | 1 |
| Red Fort | `red-fort` | USABLE | 406.2 | 18 | 1 |
| Qutb Minar | `qutb-minar` | USABLE | 98.9 | 18 | 1 |
| Lotus Temple | `lotus-temple` | USABLE | 156.5 | 18 | 1 |
| Taj Mahal | `taj-mahal` | NO_USABLE_SEGMENT | — | — | 4 |
| Fatehpur Sikri | `fatehpur-sikri` | USABLE | 304.5 | 18 | 1 |
| Amber Fort | `amber-fort` | USABLE | 267.3 | 18 | 1 |
| Hawa Mahal | `hawa-mahal` | USABLE | 40.9 | 18 | 1 |
| Jaipur City Palace | `jaipur-city-palace` | USABLE | 31.5 | 18 | 1 |
| Jantar Mantar Jaipur | `jantar-mantar-jaipur` | USABLE | 78.2 | 18 | 1 |
| Nahargarh Fort | `nahargarh-fort` | USABLE | 392.7 | 18 | 1 |
| Gateway of India | `gateway-of-india` | USABLE | 335.4 | 12 | 3 |
| Marine Drive | `marine-drive` | USABLE | 4.1 | 18 | 1 |
| Chhatrapati Shivaji Maharaj Terminus | `chhatrapati-shivaji-maharaj-terminus` | USABLE | 62 | 18 | 1 |
| Elephanta Caves | `elephanta-caves` | NO_USABLE_SEGMENT | — | — | 4 |
| Sanjay Gandhi National Park | `sanjay-gandhi-national-park` | NO_USABLE_SEGMENT | — | — | 4 |
| Kanheri Caves | `kanheri-caves` | NO_USABLE_SEGMENT | — | — | 4 |
| Haridwar | `haridwar` | USABLE | 634.7 | 18 | 1 |
| Rishikesh | `rishikesh` | NO_USABLE_SEGMENT | — | — | 4 |
| Mussoorie | `mussoorie` | USABLE | 4.5 | 18 | 1 |
| Nainital | `nainital` | USABLE | 5.1 | 18 | 1 |
| Shimla Ridge | `shimla-ridge` | USABLE | 170.5 | 18 | 1 |
| Golden Temple | `golden-temple` | USABLE | 349 | 12 | 3 |
| Manali Mall Road | `manali-mall-road` | USABLE | 20.3 | 18 | 1 |
| Darjeeling Mall | `darjeeling-mall` | USABLE | 124 | 18 | 1 |
| Varanasi Ghats | `varanasi-ghats` | NO_USABLE_SEGMENT | — | — | 4 |
| Charminar | `charminar` | USABLE | 25.2 | 18 | 1 |
| Mysore Palace | `mysore-palace` | USABLE | 350.8 | 18 | 1 |
| Hampi Virupaksha Temple | `hampi-virupaksha-temple` | USABLE | 405.1 | 12 | 3 |
| Shore Temple | `shore-temple` | NO_USABLE_SEGMENT | — | — | 4 |
| Pondicherry Promenade | `pondicherry-promenade` | USABLE | 60.1 | 18 | 1 |
| Meenakshi Temple | `meenakshi-temple` | USABLE | 131.6 | 18 | 1 |
| Munnar | `munnar` | USABLE | 2.8 | 18 | 1 |
| Ooty | `ooty` | USABLE | 353.9 | 18 | 1 |
| Kodaikanal | `kodaikanal` | USABLE | 355.9 | 18 | 1 |
| Goa Calangute Beach | `goa-calangute-beach` | USABLE | 608.8 | 18 | 1 |
| Cherai Beach | `cherai-beach` | USABLE | 69.6 | 18 | 1 |
| Kovalam Beach | `kovalam-beach` | USABLE | 64.7 | 18 | 1 |
| Brihadeeswarar Temple | `brihadeeswarar-temple` | USABLE | 170.1 | 18 | 1 |
| City Palace Udaipur | `city-palace-udaipur` | USABLE | 247.8 | 15 | 2 |
| Mehrangarh Fort | `mehrangarh-fort` | USABLE | 532.1 | 12 | 3 |
| Jodhpur Blue City Views | `jodhpur-blue-city-views` | USABLE | 18.4 | 18 | 1 |
| Jaisalmer Fort | `jaisalmer-fort` | USABLE | 366.5 | 18 | 1 |
| Pushkar | `pushkar` | USABLE | 13.5 | 18 | 1 |
| Sabarmati Ashram | `sabarmati-ashram` | USABLE | 83.2 | 18 | 1 |
| Statue of Unity | `statue-of-unity` | USABLE | 347.7 | 18 | 1 |
| Howrah Bridge | `howrah-bridge` | USABLE | 146.2 | 18 | 1 |
| Victoria Memorial | `victoria-memorial` | USABLE | 282.4 | 18 | 1 |
| Belur Math | `belur-math` | USABLE | 464.4 | 18 | 1 |
| Shaniwar Wada | `shaniwar-wada` | USABLE | 69.6 | 18 | 1 |
| Aga Khan Palace | `aga-khan-palace` | USABLE | 141.1 | 18 | 1 |
| Lonavala | `lonavala` | USABLE | 20.7 | 18 | 1 |
| Keoladeo National Park | `keoladeo-national-park` | NO_USABLE_SEGMENT | — | — | 4 |
| Khajuraho Temples | `khajuraho-temples` | USABLE | 210.9 | 12 | 3 |
| Konark Sun Temple | `konark-sun-temple` | USABLE | 308.2 | 18 | 1 |
| Ujjain | `ujjain` | USABLE | 14.3 | 18 | 1 |
| Tirupati | `tirupati` | USABLE | 26.1 | 18 | 1 |
| Kanyakumari | `kanyakumari` | USABLE | 177.3 | 18 | 1 |
| Gokarna | `gokarna` | USABLE | 227.3 | 18 | 1 |
| Junagarh Fort | `junagarh-fort` | USABLE | 173.8 | 18 | 1 |

Machine-readable copy: `docs/data/tomtom-pilot-coverage-results.csv`.

## Verified slugs for allowlist (usable only)

- `india-gate`
- `red-fort`
- `qutb-minar`
- `lotus-temple`
- `fatehpur-sikri`
- `amber-fort`
- `hawa-mahal`
- `jaipur-city-palace`
- `jantar-mantar-jaipur`
- `nahargarh-fort`
- `gateway-of-india`
- `marine-drive`
- `chhatrapati-shivaji-maharaj-terminus`
- `haridwar`
- `mussoorie`
- `nainital`
- `shimla-ridge`
- `golden-temple`
- `manali-mall-road`
- `darjeeling-mall`
- `charminar`
- `mysore-palace`
- `hampi-virupaksha-temple`
- `pondicherry-promenade`
- `meenakshi-temple`
- `munnar`
- `ooty`
- `kodaikanal`
- `goa-calangute-beach`
- `cherai-beach`
- `kovalam-beach`
- `brihadeeswarar-temple`
- `city-palace-udaipur`
- `mehrangarh-fort`
- `jodhpur-blue-city-views`
- `jaisalmer-fort`
- `pushkar`
- `sabarmati-ashram`
- `statue-of-unity`
- `howrah-bridge`
- `victoria-memorial`
- `belur-math`
- `shaniwar-wada`
- `aga-khan-palace`
- `lonavala`
- `khajuraho-temples`
- `konark-sun-temple`
- `ujjain`
- `tirupati`
- `kanyakumari`
- `gokarna`
- `junagarh-fort`
