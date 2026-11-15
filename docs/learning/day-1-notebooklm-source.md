# Building Please Don't Come Here — Day 1 Source Material for NotebookLM

**Audience:** Beginner-to-intermediate Java developers learning Spring Boot.  
**Project:** Please Don't Come Here (PDCM) — tourist crowd & traffic advisory platform.  
**Scope of this document:** Day 1 backend only, based on the actual repository implementation.

**Status facts (do not invent more):**
- Working Spring Boot API for destinations (list, search/filter, detail, nearby).
- PostgreSQL 16 via Docker Compose; 20 seeded Indian destinations.
- **Not built yet:** React frontend, user authentication, JWT, crowd reports, traffic aggregation, forecasts, admin write APIs, Swagger UI.

Use sections 1–7 as the backbone of a concise 3–5 minute Video Overview. Sections 8–10 support deeper study.

---

## 1. What we built

**Please Don't Come Here** helps travelers understand tourist destinations before visiting. Day 1 builds the **foundation**: a backend that can store and serve destination data.

**Architecture (Day 1):** a single Spring Boot application (`pdcm-api`) talks to PostgreSQL. There is no frontend in the repo yet. Clients (browser, curl, or a future React app) call HTTP APIs on port **8080**.

**Day 1 functionality that exists:**
- Browse destinations with pagination.
- Search by text (`q`), filter by `region` and `category`.
- Get one destination by id.
- Find nearby destinations using latitude/longitude and the Haversine distance formula.
- Schema and seed data applied automatically by Flyway on startup.

**Why a backend first?** Destination data must live in a durable database, be queryable consistently, and later support authentication and reports. Starting with a clean API layer means Day 2 (JWT + reports) plugs into a proven request path.

**Main package:** `com.pdcm`, with feature code under `com.pdcm.destination` and shared helpers under `com.pdcm.common`.

---

## 2. How a request travels through the application

**Example:** `GET /api/destinations`

**Terms:**
- **Controller** — HTTP entry point; maps URLs to methods.
- **Service** — business rules and orchestration.
- **Repository** — database access.
- **Entity** — Java object mapped to a database table.
- **DTO (Data Transfer Object)** — shape of the JSON we return to clients (we use Java `record`s).

### Flow

1. HTTP request hits Spring MVC.
2. `DestinationController.list(...)` receives query params.
3. Controller calls `DestinationService.search(...)`.
4. Service normalizes filters, then calls `DestinationRepository.search(...)`.
5. Hibernate/JPA turns the JPQL query into SQL against PostgreSQL.
6. Rows become `Destination` entities.
7. Service maps each entity to `DestinationResponse` via `DestinationResponse.from(...)`.
8. Spring serializes the `Page<DestinationResponse>` to JSON.

### Real controller excerpt

File: `backend/src/main/java/com/pdcm/destination/DestinationController.java`

```java
@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

	private final DestinationService destinationService;

	public DestinationController(DestinationService destinationService) {
		this.destinationService = destinationService;
	}

	@GetMapping
	public Page<DestinationResponse> list(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String region,
			@RequestParam(required = false) DestinationCategory category,
			@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
		return destinationService.search(q, region, category, pageable);
	}
}
```

**Line-by-line (important parts):**
- `@RestController` — this class handles HTTP and returns response bodies (JSON), not HTML views.
- `@RequestMapping("/api/destinations")` — all methods share this URL prefix.
- `private final DestinationService destinationService` — the controller depends on the service; it does not talk to the database.
- Constructor injection — Spring creates `DestinationService` and passes it in. The field is `final`, so it must be set once.
- `@GetMapping` on `list` — handles `GET /api/destinations`.
- `@RequestParam(required = false)` — `q`, `region`, and `category` are optional.
- `@PageableDefault(...)` — if the client omits paging params, default page size is 20, sorted by `name` ascending.
- The method body only delegates to the service — no SQL, no Haversine, no mapping logic here.

**Detail and nearby endpoints (same controller):**
- `GET /api/destinations/{id}` → `get` → `destinationService.getById(id)`
- `GET /api/destinations/{id}/nearby` → `nearby` → `destinationService.findNearby(id, radiusKm, limit)`

**Errors:** `GlobalExceptionHandler` (`@RestControllerAdvice`) maps `NotFoundException` to HTTP 404 with an `ApiError` body, and validation/`IllegalArgumentException` to HTTP 400.

---

## 3. Understanding the Spring Boot layers

### Why separate layers?

| Layer | Our class | Responsibility |
|---|---|---|
| Controller | `DestinationController` | HTTP mapping, params, status via exceptions |
| Service | `DestinationService` | Validation rules, search normalization, nearby ranking |
| Repository | `DestinationRepository` | JPQL/SQL access |
| Entity | `Destination` | Table mapping |
| DTO | `DestinationResponse`, `NearbyDestinationResponse` | API JSON shape |

If SQL lived in the controller, endpoints would become hard to test and reuse. If entities were returned directly, the API would leak persistence details and become brittle when the schema changes.

### Service

File: `backend/src/main/java/com/pdcm/destination/DestinationService.java`

```java
@Service
@Transactional(readOnly = true)
public class DestinationService {

	private final DestinationRepository destinationRepository;

	public DestinationService(DestinationRepository destinationRepository) {
		this.destinationRepository = destinationRepository;
	}
```

- `@Service` — marks this as a Spring-managed business component (**bean**).
- `@Transactional(readOnly = true)` — all public methods run in a read-only database transaction by default (appropriate for Day 1 read APIs).
- **Constructor injection** again — Spring injects a `DestinationRepository` implementation it generates at runtime.

**Dependency injection (DI):** instead of `new DestinationRepository(...)` inside the service, Spring wires collaborators. That makes unit tests able to pass a mock repository (see `DestinationServiceTest`).

### Repository

File: `backend/src/main/java/com/pdcm/destination/DestinationRepository.java`

```java
public interface DestinationRepository extends JpaRepository<Destination, Long> {

	Optional<Destination> findByIdAndActiveTrue(Long id);

	@Query("""
			SELECT d FROM Destination d
			WHERE d.active = true
			  AND (:qPattern IS NULL OR LOWER(d.name) LIKE :qPattern
			       OR LOWER(d.description) LIKE :qPattern)
			  AND (:regionLower IS NULL OR LOWER(d.region) = :regionLower)
			  AND (:category IS NULL OR d.category = :category)
			""")
	Page<Destination> search(
			@Param("qPattern") String qPattern,
			@Param("regionLower") String regionLower,
			@Param("category") DestinationCategory category,
			Pageable pageable);
```

- Extending `JpaRepository<Destination, Long>` gives basic CRUD for free.
- `findByIdAndActiveTrue` is a **derived query method** — Spring Data builds the query from the method name.
- `@Query` with JPQL (Java Persistence Query Language) expresses custom search. JPQL talks about entities (`Destination d`), not raw table names.
- Comment in code: callers must pre-normalize patterns because applying `LOWER(:param)` on null binds caused Postgres `lower(bytea)` errors during Day 1.

### Entity

File: `backend/src/main/java/com/pdcm/destination/Destination.java`

```java
@Entity
@Table(name = "destinations")
public class Destination {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private DestinationCategory category;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "destination_tags", joinColumns = @JoinColumn(name = "destination_id"))
	@Column(name = "tag", nullable = false, length = 80)
	private Set<String> tags = new HashSet<>();
```

- `@Entity` / `@Table` — this class maps to table `destinations`.
- `@Id` + `IDENTITY` — Postgres `BIGSERIAL` generates ids.
- `@Enumerated(EnumType.STRING)` — stores enum names like `HISTORICAL`, not ordinal numbers (safer if enum order changes).
- Tags use `@ElementCollection` (explained in section 4).
- Protected no-arg constructor is required by JPA.

### DTO

File: `backend/src/main/java/com/pdcm/destination/DestinationResponse.java`

```java
public record DestinationResponse(
		Long id,
		String name,
		String slug,
		// ...
		Set<String> tags,
		Instant createdAt,
		Instant updatedAt
) {
	public static DestinationResponse from(Destination d) {
		return new DestinationResponse(
				d.getId(),
				// ...
				new TreeSet<>(d.getTags()),
				d.getCreatedAt(),
				d.getUpdatedAt());
	}
}
```

A **record** is an immutable data carrier. `from` copies entity fields into the API model and sorts tags with `TreeSet` for stable JSON ordering.

`NearbyDestinationResponse` wraps a `DestinationResponse` plus `distanceKm`.

### Category enum

`DestinationCategory`: `HISTORICAL`, `TEMPLE`, `BEACH`, `HILL_STATION`, `WILDLIFE`, `CITY`, `NATURE`.

---

## 4. Database and Flyway

**PostgreSQL** is our relational database: structured tables, constraints, indexes, and SQL that interviewers expect you to understand.

**Flyway** applies versioned SQL scripts on startup so every environment gets the same schema history.

### V1 — schema

File: `backend/src/main/resources/db/migration/V1__create_destinations.sql`

Creates:
- `destinations` — core fields including `slug` (unique), lat/lon with CHECK constraints, optional `crowd_sensitivity`, `active`, UTC timestamps (`timestamptz`).
- Indexes on `region`, `category`, `active`.
- `destination_tags` — `(destination_id, tag)` primary key, FK to `destinations` with `ON DELETE CASCADE`.

### V2 — seed data

File: `backend/src/main/resources/db/migration/V2__seed_destinations.sql`

- Inserts **20** Indian destinations (Taj Mahal, Gateway of India, Jaipur sites, beaches, hill stations, etc.).
- Inserts tags by joining destination `slug` to a VALUES list of `(slug, tag)` pairs.

### Why Flyway owns schema and Hibernate uses `validate`

From `backend/src/main/resources/application.yml`:

```yaml
jpa:
  hibernate:
    ddl-auto: validate
flyway:
  enabled: true
```

- **`ddl-auto=validate`** — Hibernate checks that entities match the existing schema; it does **not** create or alter tables.
- **Flyway** is the deliberate history of schema changes (`V1`, `V2`, …).
- Alternative `update`/`create` would hide migration discipline and risk surprising production changes. We rejected that for a portfolio/interview-ready project.

### `@ElementCollection` and `destination_tags`

Tags are not a first-class `Tag` entity with its own id. They are a collection of strings owned by `Destination`.

JPA maps that to join table `destination_tags(destination_id, tag)`.

**Why this approach:** simple, portable, easy to query with ordinary joins. We intentionally avoided PostgreSQL `text[]` for MVP to reduce dialect/JPA friction.

**Limitation:** `FetchType.EAGER` loads tags whenever a destination is loaded. Fine for ~20 rows; for large datasets we would revisit fetch strategy.

---

## 5. Search, filtering and nearby destinations

### Search / filter

In `DestinationService.search`:

```java
String qPattern = blankToNull(q) == null ? null : "%" + blankToNull(q).toLowerCase() + "%";
String regionLower = blankToNull(region) == null ? null : blankToNull(region).toLowerCase();
return destinationRepository.search(qPattern, regionLower, category, pageable)
		.map(DestinationResponse::from);
```

- Blank strings become `null` so “no filter” means the JPQL `:param IS NULL` branch.
- Text search is case-insensitive `LIKE` on name **or** description.
- Region compared case-insensitively; category compared as enum.
- Only `active = true` destinations are returned.

**Limitation:** nearby search is not done in SQL; search does not filter by tags in Day 1.

### Nearby (Haversine)

**Haversine formula:** estimates great-circle distance between two points on a sphere using latitude and longitude. We use mean Earth radius **6371 km** in `GeoUtils.haversineKm`.

File: `backend/src/main/java/com/pdcm/common/GeoUtils.java`

```java
public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
	double dLat = Math.toRadians(lat2 - lat1);
	double dLon = Math.toRadians(lon2 - lon1);
	double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(dLon / 2) * Math.sin(dLon / 2);
	double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	return EARTH_RADIUS_KM * c;
}
```

In `DestinationService.findNearby`:
1. Validate `radiusKm > 0` and `limit` in 1..50.
2. Load origin with `requireActive(id)`.
3. Load other active destinations via `findActiveExcluding(id)`.
4. Compute distance for each candidate.
5. Filter by radius, sort ascending by distance, limit results.
6. Round distance to one decimal kilometre.

**Why not PostGIS?** For MVP ranking within a few hundred kilometres and ~20 destinations, in-memory Haversine is enough, simpler to operate, and easier to explain. PostGIS would help at larger scale with spatial indexes — a future improvement, not Day 1.

**Limitation:** loads all other active destinations into the app to rank them. Acceptable for seed size; not ideal for millions of rows.

---

## 6. Docker and PostgreSQL

### Compose

File: `docker-compose.yml` (repo root)

- Image: `postgres:16`
- Container name: `pdcm-postgres`
- Database/user/password: `pdcm` / `pdcm` / `pdcm`
- Host port mapping: **`5433:5432`**
- Named volume `pdcm_pg_data` for persistence
- Healthcheck with `pg_isready`

**Why port 5433?** On the development machine, a local Windows PostgreSQL already listened on **5432**. Mapping Docker to 5433 avoids authentication failures against the wrong server.

### Application connection

`application.yml`:

```yaml
datasource:
  url: jdbc:postgresql://localhost:5433/pdcm
  username: pdcm
  password: pdcm
  hikari:
    connection-init-sql: SET TIME ZONE 'UTC'
server:
  port: 8080
```

- **JDBC URL** points at Docker Postgres on 5433.
- **HikariCP** is Spring Boot’s default connection pool; `connection-init-sql` forces UTC session timezone (Windows `Asia/Calcutta` previously broke Postgres connections).
- Spring Boot Docker Compose auto-start is **disabled** by default to avoid fighting the root compose file; optional profile `docker` can enable `backend/compose.yaml`.

**Typical run:** `docker compose up -d` then `./mvnw spring-boot:run` from `backend/`.

---

## 7. Testing

Day 1 executed **13 tests, 0 failures**.

### Unit tests (fast, no database)

**`GeoUtilsTest` (3 tests)**
- Same point → distance 0.
- Delhi → Agra roughly ~180 km (tolerance window).
- Jaipur cluster points are under 1 km apart.

**Proves:** Haversine math.  
**Does not prove:** HTTP or database wiring.

**`DestinationServiceTest` (3 tests, Mockito)**
- Missing id → `NotFoundException`.
- Invalid radius / limit → `IllegalArgumentException`.

**Proves:** service validation and not-found behavior with a mocked repository.  
**Does not prove:** real SQL or Flyway seed content.

### Integration tests (Spring + Testcontainers Postgres)

**`DestinationIntegrationTest` (6 tests)**
- List returns 20 seeded destinations.
- Region+category filter (Rajasthan + HISTORICAL → 3).
- Query `q=taj` → `taj-mahal`.
- Get by id returns detail.
- Unknown id → 404 `NOT_FOUND`.
- Nearby around Hawa Mahal within 20 km includes Jaipur City Palace and Amber Fort.

**`PdcmApiApplicationTests` (1 test)**
- Spring context loads against Testcontainers Postgres.

**Proves:** real HTTP → service → JPA → Postgres path, migrations, and seed data.  
**Does not prove:** frontend behavior, security, or crowd features (none exist yet).  
**Requires:** Docker available for Testcontainers.

---

## 8. Important technical decisions

| Decision | Choice | Alternative | Why we chose this |
|---|---|---|---|
| App style | Modular monolith | Microservices | One deployable for a 7-day MVP |
| Schema ownership | Flyway + `validate` | Hibernate `update` | Explicit, reviewable migrations |
| Tags | `@ElementCollection` join table | `text[]` or Tag entity | Simple and portable |
| Geo | Haversine in Java | PostGIS | Enough for MVP scale |
| API model | DTOs / records | Return entities | Stable API, hide persistence |
| DI | Constructor injection | Field `@Autowired` | Testable, immutable dependencies |
| Compose port | Host 5433 | 5432 | Avoid local Windows Postgres clash |
| Boot compose | Disabled by default | Always auto-start | Prevent double containers / port fights |
| Day 1 security | None yet | JWT from day 1 | Auth is Day 2; keep Day 1 focused |

**Future (not Day 1):** JWT auth, visit reports, crowd/traffic engines, React dashboard, Swagger, Redis, cloud deploy.

---

## 9. Interview preparation

**Q1. What happens when a client calls `GET /api/destinations`?**  
A: Spring routes to `DestinationController.list`, which calls `DestinationService.search`, which calls `DestinationRepository.search`, Hibernate runs SQL on PostgreSQL, entities map to `DestinationResponse`, JSON is returned as a Spring Data `Page`.

**Q2. Why not put the JPQL query in the controller?**  
A: Controllers should stay thin. Business/query orchestration belongs in the service/repository so logic is reusable and unit-testable without HTTP.

**Q3. What is constructor injection?**  
A: Dependencies are passed into the constructor. Spring creates beans and wires them. Fields can be `final`. In tests we can construct the service with a mock repository.

**Q4. Entity vs DTO?**  
A: `Destination` is a JPA entity tied to the table. `DestinationResponse` is the API contract. Separating them prevents leaking persistence fields and lets the API evolve independently.

**Q5. Why Flyway instead of `ddl-auto=update`?**  
A: Flyway versions schema changes as code. `validate` ensures entities match that schema without silently altering production databases.

**Q6. How do tags work without a Tag entity?**  
A: `@ElementCollection` maps `Set<String> tags` to `destination_tags(destination_id, tag)`.

**Q7. Explain Haversine in one sentence.**  
A: It estimates distance between two lat/lon points on a sphere; we use it in `GeoUtils.haversineKm` for nearby ranking.

**Q8. Why port 5433?**  
A: Docker Postgres is published on host 5433 because local Windows Postgres already used 5432; the app JDBC URL targets 5433.

**Q9. Unit vs integration test in this project?**  
A: `GeoUtilsTest` / `DestinationServiceTest` are unit-level (pure or mocked). `DestinationIntegrationTest` boots Spring and uses Testcontainers PostgreSQL for real HTTP+DB verification.

**Q10. What is still missing after Day 1?**  
A: Authentication/JWT, reports, crowd/traffic aggregation, forecasts, React UI, and admin write endpoints.

---

## 10. Knowledge check

Answer without looking first, then verify against the code.

1. Name the four main types involved in `GET /api/destinations/{id}` from HTTP to JSON (`Controller`, service method, repository method, DTO).
2. Why does `DestinationService.search` build `%query%` in Java instead of calling `LOWER(:q)` inside JPQL?
3. What do `V1` and `V2` Flyway files each do?
4. In `findNearby`, what happens if `limit` is 0 or 100?
5. Which test class would still pass if PostgreSQL were completely unavailable, and which would fail?

**Expected answers (study key):**
1. `DestinationController.get` → `DestinationService.getById` → `findByIdAndActiveTrue` → `DestinationResponse`.
2. Null parameter binding with `LOWER(:param)` caused Postgres `lower(bytea)` errors; normalize in the service and keep JPQL null-safe.
3. V1 creates tables/indexes; V2 inserts 20 destinations and tags.
4. `IllegalArgumentException` (limit must be 1..50); handled as HTTP 400 by `GlobalExceptionHandler`.
5. `GeoUtilsTest` (and mocked `DestinationServiceTest`) can pass without Postgres; `DestinationIntegrationTest` / context test need Docker/Testcontainers.

---

## NotebookLM usage tip

For a **3–5 minute Video Overview**, ask NotebookLM to emphasize: product purpose → request flow with real class names → Flyway vs validate → Haversine nearby → Docker port 5433 → 13 green tests → Day 2 preview (auth + reports). Keep “not built yet” items explicit so the video stays honest.
