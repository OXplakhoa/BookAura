# REQUIREMENTS — Frozen Scope (Assignment 1, deadline 2026-08-25)

Product: **BookAura** — library management system. Browse → borrow → return → history.
NOT a bookstore: no cart, checkout, payment, shipping, orders.

## Implementation status (verified 2026-08-25)

- ✅ Bootstrap + Auth Core (P0-A #1–9): implemented; auth regression suite passing.
- ✅ Book backend (P0-A #10–14): CRUD, soft delete, ISBN validation, Specification filters,
  page size max 10, multi-sort allowlist, CSV policy + row errors + transactional rollback proof.
- ✅ Member backend (P0-A #15–16): CRUD/disable, seven-condition Specification search including
  real borrowed-book title, strict `yyyy/MM/d`, pagination/sort validation.
- ✅ Loan backend (P0-A #17–20): borrow/return, active/history, final-copy atomic concurrency,
  duplicate guards, USER ownership + ADMIN override, borrow/return rollback proofs.
- ✅ Maintenance backend (#21): cached flag, 503 error contract, ADMIN control + health exceptions.
- ✅ Observability baseline (#25–28): Log4j2 rolling file/console, traceId, bounded redacted HTTP logs,
  audit events and annotation-driven AOP outcome/duration logs.
- ✅ Core React frontend (#31): auth/session restoration, URL-backed public catalog/detail,
  member borrow/active/return/history, ADMIN Book/CSV/Member/Loan/Maintenance workspaces,
  responsive/a11y states and route splitting.
- ✅ Google OAuth P0-B (#32): optional Google OIDC registration, verified-email identity link/create,
  60-second hashed single-use exchange code, normal access/refresh session and React callback cleanup.
  Domain/filter wiring is tested without secrets; real Google consent remains credential-dependent.
- ✅ Email OTP/change email P0-B (#33): authenticated new-email request, six-digit delivery,
  SHA-256, 10-minute expiry, 60-second cooldown, committed five-attempt lockout, atomic single-use
  confirmation and account settings UI.
- ✅ Mocked phone OTP P0-B (#34): enumeration-safe request, in-memory `FakeSmsSender`,
  five-minute hashed code, silent cooldown, five-attempt/single-use confirmation, normal app session,
  phone login UI and ADMIN-only local outbox with no raw OTP logs.
- ✅ Facebook OAuth P0-B (#35): completed 2026-08-24 (D29).
- Evidence: clean `./mvnw verify` = **75 backend tests, 0 failures/errors**; frontend **36 tests**,
  Oxlint, TypeScript and production build pass. Live through Vite proxy: login/refresh, all ADMIN list endpoints,
  Book create/update/archive, CSV multipart import/archive, Member create/update/disable and
  maintenance 200→503→200 all passed.

## P0-A — Must work (demo-critical)

| # | Requirement | Acceptance highlight |
|---|-------------|----------------------|
| 1 | Project bootstrap (Maven wrapper, profiles, Liquibase, Log4j2, error format) | Empty DB boots via migrations; backend+frontend build |
| 2 | Spring Security + JWT | Access JWT 15min (jti, sub, roles, iat, exp) |
| 3 | Email/password registration | Invalid → 400; duplicate email → 409 |
| 4 | Email verification (real email via Brevo; Mailpit local) | Unverified account cannot log in (401 `EMAIL_NOT_VERIFIED`) |
| 5 | Login (email or normalized phone) | Issues app-level JWT + refresh cookie |
| 6 | Access token | Short-lived, frontend memory only |
| 7 | Refresh token | Rotating, HttpOnly cookie, hash stored, reuse revokes family |
| 8 | Logout token invalidation | Revokes refresh session + records access-token jti until exp; old token → 401 |
| 9 | ADMIN / USER authorization | `@PreAuthorize`; USER → ADMIN API = 403 |
| 10–14 | Book CRUD + Specification search + pagination (max 10) + sort allowlist + CSV import | CSV: <5MB, header+row validation, duplicate ISBN via Set, all-or-nothing rollback |
| 15–16 | Member CRUD + search ≥5 conditions | name LIKE, email/phone, DoB range `yyyy/MM/d`, borrowed book, status |
| 17–20 | Borrow/return + inventory validation + transactions | Atomic conditional decrement; 1 active loan/book/user; duplicate return rejected; rollback proven by tests |
| 21 | Maintenance mode | Business APIs → 503; exceptions: admin control endpoint + health (documented) |
| 22–23 | Validation + exception handling | Consistent error JSON incl. `traceId`, `validationErrors` |
| 24 | Hibernate relationships | User 1–1 Profile, Profile 1–n Loan, Book n–n Author/Category, Loan n–1 Book |
| 25–28 | Lombok / Log4j2 / AOP / Liquibase | No `@Data` on entities; console+rolling file logs; redaction; AOP timing+audit |
| 29 | OpenAPI | springdoc, bearer JWT scheme, examples |
| 30 | Representative tests | JUnit5 + Mockito + MockMvc + Testcontainers (see test plan) |
| 31 | Core React frontend for mentor demo | Auth screens, catalog, loans, admin books/members, maintenance UI |

## P0-B — Required by mentor note, must not block P0-A (in this order)

32. ✅ Google OAuth (Spring Security OAuth2 Client; backend callback → one-time code → session; no tokens in URL)
33. ✅ Email OTP / change email (code to new email, change only after verify)
34. ✅ Phone OTP with mocked `SmsSender` (full OTP lifecycle: hash, expiry, cooldown, attempt limit, one-time, purpose, audit)
35. ~~Facebook OAuth (same architecture; document and skip if it becomes a time sink)~~ ✅ done 2026-08-24 (D29: OAuth2 + Graph `/me`, credential-conditional)

## P2 — Wow features (cut from bottom upward under time pressure)

36. ~~Shelf Aura — deterministic `RuleBasedRecommendationEngine` (score + human-readable reasons + matched tags)~~ ✅ done 2026-08-25 (D30)
37. ~~3D bookshelf (lazy chunk, 2D fallback, `prefers-reduced-motion`)~~ ✅ done 2026-08-25 (D32)
38. Real SMS gateway
39. Experimental AI (`EmbeddingRecommendationEngine` stub behind interface + feature flag only)

## Explicit exclusions (Assignment 1)

Microservices/Eureka/Gateway/Kafka; e-commerce flows; Supabase Auth as identity authority;
frontend direct DB access; production deployment (local demo is acceptable); >1 gamification feature.
