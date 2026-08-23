# ARCHITECTURE

## Style

**Modular monolith** — one Spring Boot deployable, package-by-feature. No microservices.

```
com.bookaura
  common/        config, error (GlobalExceptionHandler), logging (TraceIdFilter, AOP), security base, web
  auth/          controller, dto, entity, repository, service, token (JWT), oauth, otp
  account/       profile, change password/email
  catalog/       book, author, category + specification + importcsv
  member/        admin member management
  loan/          borrow/return, inventory
  systemconfig/  maintenance mode + filter
  recommendation/ RecommendationEngine + RuleBasedRecommendationEngine (P2)
```

Rules: thin controllers · business logic in `@Transactional` services · repositories = persistence only ·
explicit DTO mapping (no entities over the wire) · security rules centralized + `@PreAuthorize`.

## Request pipeline

```
HTTP → TraceIdFilter (MDC traceId + X-Trace-Id header)
     → MaintenanceFilter (503 for business APIs when ON; whitelist: admin config, /actuator/health)
     → JwtAuthenticationFilter (Bearer JWT → Authentication; checks revoked jti)
     → SecurityFilterChain authorization → @PreAuthorize → Controller → Service (@Transactional) → Repository
```

## Data / ownership

- PostgreSQL 16. Liquibase is the **only** schema owner; `ddl-auto=validate`.
- Local: Docker (`infra/docker-compose.yml`); tests: Testcontainers; demo: Supabase Postgres.
- React never touches the DB — all data via Spring Boot API.

## Auth model (summary — details in AUTH_FLOW.md)

- Access JWT 15 min (HS256, env secret), claims: `jti`, `sub`, `roles`, `iat`, `exp`. Stored **in frontend memory only**.
- Refresh token: opaque random, 7 days, **HttpOnly + SameSite=Lax cookie** (`path=/api/auth`), rotation + reuse detection (revoke family), only SHA-256 hash persisted.
- Logout: revoke refresh session + insert `revoked_access_tokens(jti, expires_at)`.

## Deployment model (demo = local)

```
React SPA (localhost:5173) ──HTTPS/HTTP──▶ Spring Boot (localhost:8080) ──▶ PostgreSQL
                                              │                                (Docker local /
                              Brevo SMTP (email verification)                Supabase demo)
```

`Dockerfile` + `docker-compose` exist for portability; no Render/Railway/Vercel unless P0 done.

## Transaction boundaries

| Use case | Boundary | Rollback demo |
|----------|----------|---------------|
| `LoanService.borrow` | `@Transactional`: conditional inventory decrement + insert Loan | forced failure test rolls back both |
| `LoanService.returnOwn/returnAsAdmin` | `@Transactional`: conditional NULL→returned update + conditional inventory increment | forced failure rolls both mutations back; duplicate return affects 0 rows |
| `CsvImportService` | one TX: bulk relation resolution + `saveAll+flush` | test-only post-flush failure rolls books/authors/categories back |

Concurrency uses one concept only: **atomic conditional update + affected-row count**.
Borrow decrements only when `available_quantity > 0`; return changes only an active loan and increments only
when `available_quantity < total_quantity`. No pessimistic/optimistic lock combination.
The partial unique index on `(member_profile_id, book_id) WHERE returned_at IS NULL` closes the concurrent
same-member/same-book race; a losing transaction rolls its inventory decrement back.

## Logging / observability

Implemented: Log4j2 console + rolling file, MDC `traceId`, security/business audit logger.
Pending observability slice: redacted request/response logging and AOP service duration logging.
Passwords/tokens/OTP/Authorization are never logged.
