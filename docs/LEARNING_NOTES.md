# Learning Notes

Interview-oriented checklist. Concepts are filled in as we build each milestone — not a dump of every annotation.

## Checklist

### Java
- [ ] OOP vs C++ mental model (references, GC, equals/hashCode)
- [ ] Generics, Optional, records
- [ ] Collections (List/Map/Set) and when to use each
- [ ] Exceptions vs error responses

### Spring Boot
- [ ] Request lifecycle: filter → controller → service → repository
- [ ] Constructor injection
- [ ] `@Transactional` boundaries
- [ ] DTO vs entity
- [ ] Centralized exception handling
- [ ] SecurityFilterChain + JWT validation (Security 7 lambda DSL)

### SQL / PostgreSQL
- [ ] Flyway migrations vs `ddl-auto`
- [ ] FKs, unique constraints, indexes
- [ ] `timestamptz` and timezone conversion
- [ ] Advisory locks (why vs Redis for this MVP)

### REST
- [ ] Idempotency expectations
- [ ] 401 vs 403 vs 429
- [ ] Pagination and filtering

### Testing
- [ ] Unit test pure aggregation (no Spring)
- [ ] `@SpringBootTest` + Testcontainers
- [ ] Testing concurrency/rate limits

### System design
- [ ] Modular monolith trade-offs
- [ ] Compute-on-read vs materialized scores
- [ ] Honest uncertainty in product APIs

### Git / delivery
- [ ] Meaningful commits, `.gitignore`, README as portfolio surface

### Cloud / AI (later)
- [ ] Cost-aware deploy decisions
- [ ] When not to use an LLM (deterministic scoring)

## After each milestone

1. Explain the problem and chosen approach in your own words.  
2. Complete one small coding exercise.  
3. Short interview (one question at a time).  
