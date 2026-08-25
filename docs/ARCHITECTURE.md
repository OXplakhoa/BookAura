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
- Authenticated email change reuses purpose-bound `otp_tokens`: the latest CHANGE_EMAIL token is tied to
  user + new-email target, wrong attempts commit independently, and correct consumption is an atomic update.
- Google OIDC is optional: Spring validates state/nonce/signature/provider claims, then the backend redirects
  only a 60-second opaque exchange code. DB stores its SHA-256 hash; atomic consume issues the same app
  session as password login. JWT/provider tokens never enter redirect URLs. The OAuth handshake uses a
  transient `JSESSIONID` only for authorization state and invalidates it immediately after callback.

## Frontend architecture

```text
BrowserRouter
  ├─ PublicLayout: landing → URL-backed catalog → book detail
  ├─ AuthLayout: registration → email verification → login
  └─ RequireAuth → AppShell
       ├─ USER: active loans → confirmed return → history
       └─ RequireAuth(admin): books / CSV / members / loans / maintenance
```

- `session-store.ts` holds access token + user in module memory only; no Web Storage token keys exist.
- Axios attaches the bearer token, serializes concurrent 401 recovery behind one refresh promise, rotates
  through the HttpOnly cookie, retries each request once and clears identity if refresh fails.
- React Query owns server state and invalidation; auth/maintenance remain small client-state contexts.
- Catalog filters, allowlisted sort and page are URL state for deep links and predictable browser Back.
- React Hook Form + Zod provide on-blur field validation; backend `ApiError.validationErrors` remain authoritative.
- ADMIN screens are lazy route chunks; responsive layouts use semantic landmarks, visible focus, 44px targets,
  reduced-motion behavior and explicit loading/error/empty/success states.
- Shelf Aura keeps its existing 2D recommendation cards as the source of truth and fallback. Eligible browsers
  can opt into a dependency-free CSS 3D shelf through a separate `React.lazy` chunk; `prefers-reduced-motion`,
  missing CSS 3D support, or a chunk/render failure selects the list view automatically.

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

## Maintenance flow

```mermaid
flowchart TD
    R[Request] --> C[CORS + JWT filters]
    C --> M{Cached maintenance flag?}
    M -- OFF --> A[Authorization + Controller]
    M -- ON --> E{Path exception?}
    E -- health --> A
    E -- /api/admin/system-config --> S[@PreAuthorize ADMIN]
    E -- OPTIONS --> A
    E -- normal API --> X[503 MAINTENANCE_MODE + traceId + Retry-After]
    S --> T[DB update in transaction]
    T --> K[afterCommit updates AtomicBoolean cache]
```

The filter performs **zero configuration DB queries per request**. Bypassing the filter does not bypass
security: system-control endpoints still require a currently valid ADMIN access token. For this local demo,
an expired ADMIN token during maintenance would require the documented operational fallback (restart with
DB flag off); widening the exception to login/refresh was rejected to keep the escape hatch narrow.

## Logging / observability

Implemented:
- Log4j2 console + rolling file (`10 MiB`, daily/size rotation, five archives).
- `TraceIdFilter` puts `traceId` in MDC, response header and every API error.
- `HttpExchangeLoggingFilter` logs method/path/status/duration only (never query strings/headers),
  suppresses auth/OAuth/infra/multipart bodies, parses and recursively redacts JSON, caps output at 2,000 chars.
- Redacted fields include passwords, tokens, OTP/codes/secrets and member PII. Malformed JSON never falls
  back to raw logging.
- `@LogOperation` AOP logs only service name, outcome and duration — never arguments/results/entities.
- Dedicated audit logger records security/business event names and entity IDs, not credentials.
