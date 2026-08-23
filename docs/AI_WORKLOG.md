# AI WORKLOG — evidence of AI-assisted development

Format: Date | Goal | Prompt summary | Files | Review points | Problems | Commands/tests | Result | Commit

---

## 2026-08-21 — Session 1: Environment + Phase 1 design artifacts

- **Goal:** prepare environment (all on F: — C: full) and create lightweight Phase 1 docs.
- **Prompt summary:** "Freeze decisions; begin Phase 1 lightweight; then bootstrap + Auth Core. Keep everything on F:."
- **Files:** `.gitignore`, `README.md`, `docs/REQUIREMENTS.md`, `docs/ARCHITECTURE.md`, `docs/ERD.md`, `docs/AUTH_FLOW.md`, `docs/WIREFRAMES.md`, `docs/DECISIONS.md`, `docs/AI_WORKLOG.md`.
- **Review points:** frozen scope (P0-A/P0-B/P2); token storage model; entity identity strategy; borrow concurrency choice; admin seed strategy (D10); maintenance whitelist (D12).
- **Problems found:** C: drive 100% full (373MB free) — JDK 17 initially installed to C: by winget; Docker daemon not running; no global Maven; no psql.
- **Corrections:** uninstalled C: JDK, installed Temurin JDK 17 ZIP to `F:\tools\jdk-17.0.20+8`; Maven 3.9.16 to `F:\tools\apache-maven-3.9.16`; npm cache → `F:\npm-cache`; verified Docker Desktop `CustomWslDistroDir=F:\DockerData\DockerDesktopWSL`; started Docker Desktop.
- **Commands/tests executed:**
  - `winget install/uninstall EclipseAdoptium.Temurin.17.JDK` (verified: install ok, uninstall ok)
  - `"F:\tools\jdk-17.0.20+8\bin\java.exe" -version` → `openjdk 17.0.20` ✓
  - `git init -b main` ✓
  - `npm config set cache F:\npm-cache --global` ✓
  - Spring Boot 3.5.x latest = `3.5.16`, Maven latest 3.9.x = `3.9.16` (queried Maven Central metadata)
- **Result:** environment F:-only; docs drafted.
- **Commit:** (see git log — reported after commit)

---

## 2026-08-21 — Session 1 (cont.): Bootstrap + Auth Core slice

- **Goal:** bootstrap (backend+frontend+infra) and the full auth-core vertical slice.
- **Prompt summary:** "Proceed directly into project bootstrap and Auth Core."
- **Files:** `backend/**` (pom, wrapper, 4 profiles, log4j2, Liquibase 0001–0007, entities, repos, security chain, JWT/refresh/cookie services, OTP service, EmailSender + SMTP/Fake impls, AuthService/AuthController, ProbeController, LocalAdminSeeder); `frontend/**` (Vite React TS + Tailwind skeleton); `infra/docker-compose.yml`; `.env.example`; tests: `AbstractIntegrationTest`, `AuthFlowIntegrationTest` (9), `JwtServiceTest` (4), `RefreshTokenServiceTest` (4).
- **Review points (checked by hand):** DTO-only API; lazy loading contained inside `@Transactional` (moved `toSummary` into TX); cookie flags HttpOnly/SameSite=Lax/path=/api/auth; OTP table enforces purpose+expiry+one-time+cooldown+attempts; admin seed profile-gated.
- **Problems found (by running tests, not by reading):**
  1. JWT `exp` is second-precision → test assertion on nanos failed → `JwtService` truncates to seconds.
  2. **Family revocation rolled back** because it shared the TX that then threw 401 → extracted `RefreshFamilyRevoker` with `REQUIRES_NEW` (D17).
  3. `@PreAuthorize` denial reached `@ControllerAdvice` (not the filter-chain handler) → added `AccessDeniedException` → 403 handler.
  4. `admin` identifier classified as phone (no `@`) → email-first lookup (D17).
  5. Stale app process on port 8080 served old code → kill by PID before restart.
- **Commands/tests executed:** `./mvnw test` → **17/17 pass** (9 Testcontainers integration + 8 unit); fresh-DB boot via Liquibase → 9 tables; live curl: health UP, admin login, /me, admin ping, refresh rotation 200, replay old cookie → 401 `REFRESH_REUSED`.
- **Result:** auth-core slice verified end-to-end (tests + live smoke).
- **Commit:** (see git log — reported after commit)

---

## 2026-08-23 — Session 2: Book Management backend slice

- **Goal:** implement P0-A Book CRUD/search/pagination/sorting/CSV import without starting frontend bonus work.
- **Prompt summary:** "Continue building; commit every completed feature clearly."
- **Files:** catalog entities/repos/DTOs/services/specifications/controllers/validators/importcsv; common page/error/security updates; Liquibase `0008-catalog-schema.yaml`; catalog unit + Testcontainers integration tests; README/requirements/ERD/decisions.
- **Important review points:**
  - Book owns unidirectional many-to-many join tables; no `CascadeType.ALL`, no entity JSON.
  - Update preserves `borrowedCopies = total - available`; new total cannot drop below borrowed copies.
  - Specification runs combined filters in SQL; page relation mapping uses `@BatchSize(50)` instead of collection-fetch pagination.
  - CSV streams records, retains only a bounded validated model (`<5 MiB`, max 10k rows), uses `Set` duplicate detection O(n), and bulk relation/ISBN lookup.
- **Problems found/corrections:** first integration run could not find Docker because `docker-desktop` WSL distro was stopped; confirmed with `docker info` + `wsl -l -v`, started Docker Desktop, reran unchanged tests successfully. Test-only forced rollback intentionally logs one stack trace while returning 500; assertions verify all three table counts unchanged.
- **Commands/tests executed:**
  - Context7 checked current Spring Data JPA Specification/Pageable and Spring multipart/transaction APIs; Apache Commons CSV streaming/header API.
  - `./mvnw -DskipTests compile` → pass.
  - Targeted catalog: **12/12 pass** (7 PostgreSQL integration + 5 unit).
  - `./mvnw verify` → **29 tests, 0 failures, 0 errors**.
  - `npm run build` → pass.
  - Local profile smoke: health `UP`; Liquibase applied `0008-catalog-schema`; 5 catalog tables present; admin created a book (201); unauthenticated multi-condition public search returned it.
- **Result:** Book Management backend P0-A slice complete; frontend screens remain pending.
- **Commits:** `cec9dd6`, `83c8e63`, `248d68f`, `263edac`; docs `54c4b7b`; merge `40d7d47`.

---

## 2026-08-23 — Session 2 (cont.): Borrow / Return slice

- **Goal:** deliver P0-A transactional loan flow before Member search so Member can later query real borrowed-book relationships.
- **Prompt summary:** continue implementation and commit each completed feature separately.
- **Files:** `loan/**`, `MemberProfileRepository`, conditional inventory methods in `BookRepository`, Liquibase `0009-loans`, loan tests, architecture/requirements/ERD/decisions/README.
- **Important review points:**
  - One concurrency strategy: conditional `UPDATE` + affected row count; no pessimistic/optimistic lock mixing.
  - DB partial unique index permits only one active loan per member/book, including concurrent requests.
  - Return updates loan only when `returned_at IS NULL`; only that winner increments inventory.
  - JPQL bulk update bypasses persistence-context synchronization; managed `Loan.returnedAt` is explicitly synchronized for response mapping.
- **Problems found/corrections:** after the first successful full suite and local smoke, Docker Desktop stopped again; a final verify therefore produced 3 Testcontainers environment errors (unit tests still passed). Logs showed WSL itself was healthy but Docker received `QuitDockerDesktop`; Bash background launch was not reliably detached. Ran `wsl --shutdown`, launched Docker with Windows `Start-Process`, verified the engine stayed alive, then reran the unchanged suite successfully. This failed run is not reported as a code failure or completion.
- **Tests/commands executed:** targeted loan **8/8 pass**; final `./mvnw verify` retry **37/37 pass**; Docker remained alive after verify; real two-thread final-copy test; forced borrow rollback; forced return rollback; local Liquibase `0009` + live borrow 201/return 200.
- **Result:** Loan P0-A backend slice complete; Member management can now search real Loan→Book data.
- **Commits:** `f74c141`, `32b012a`, `9979f60`; docs `f6ccec3`, environment evidence `a992fbb`; merge `e9a40a5`.

---

## 2026-08-23 — Session 2 (cont.): Member Management backend slice

- **Goal:** deliver ADMIN Member CRUD/disable and ≥5 composable search conditions, now backed by real Loan→Book data.
- **Files:** `member/**`; MemberProfile inverse loans relation/repository Specification support; account phone uniqueness; global Hibernate batch fetch; member tests; shared Testcontainers base fix; docs.
- **Important review points:** generic update cannot change email or role (email needs OTP verification, role needs separate privileged API); disable retains account/profile/loans; ADMIN-created member defaults unverified unless explicitly verified after in-person check.
- **Problems found/corrections:**
  1. Seven-filter query returned PostgreSQL error: `SELECT DISTINCT` cannot order by joined `user.email` absent from select list. Replaced borrowed-title join+distinct with correlated `EXISTS`; removed unnecessary role distinct. Same integration test then passed.
  2. Targeted tests passed, but full suite reused a cached Auth Spring context after inherited JUnit `@Container` had stopped; Hikari pointed to a dead random port. Reworked `AbstractIntegrationTest` to one JVM-lifetime PostgreSQL container with `DynamicPropertySource`; full suite passed.
- **Tests/commands executed:** member targeted first 6/7 (query failure), unchanged regression after fix **7/7**; first full suite exposed 5 stale-container errors; after test-infra fix final `./mvnw verify` **44/44 pass**; Docker remained alive.
- **Result:** Member backend P0-A slice complete, including borrowed-book/title criterion and strict mentor date behavior.
- **Commits:** `f03539a`, `fd81b42`, `ff02313`; docs `c7a7fc5`; merge `9fb9f70`.

---

## 2026-08-23 — Session 2 (cont.): Maintenance Mode backend slice

- **Goal:** implement operational maintenance mode without DB query per request or an admin lockout deadlock.
- **Files:** `systemconfig/**`; SecurityFilterChain update; Liquibase `0010-system-configuration`; tests; architecture flow/requirements/decisions/README/ERD.
- **Important review points:** DB is source of truth; `AtomicBoolean` is request cache; cache mutation runs in transaction `afterCommit`; filter is created inside SecurityFilterChain (not servlet auto-registration); skipped control path remains protected by class-level `@PreAuthorize`.
- **Operational risk recorded:** only a currently valid ADMIN access token can turn maintenance off. Login/refresh are intentionally not whitelisted; local fallback is restart after changing the DB flag. This keeps required exceptions narrow.
- **Tests/commands executed:** compile pass; maintenance targeted **2/2 pass**; full `./mvnw verify` **46/46 pass**; local Liquibase `0010`; live ON 200 → public catalog 503 with traceId → health 200 → OFF 200.
- **Result:** Maintenance backend P0-A complete; frontend maintenance route/interceptor remains pending.
- **Commits:** `1abc3a3`, `3b5e323`, `ccf5284`; docs `d65fac2`; merge `ea65933`.

---

## 2026-08-23 — Session 2 (cont.): Observability / AOP hardening

- **Goal:** complete P0 logging/AOP expectations while proving no passwords/tokens/OTP/PII leak.
- **Files:** `HttpExchangeLoggingFilter`, `SafePayloadSanitizer`, `LogOperation`, `ServiceOperationLoggingAspect`; service annotations; auth/SMTP audit cleanup; logging tests; docs.
- **Important review points:** filter never logs headers/query strings; auth/OAuth/infra/multipart bodies are not wrapped; non-sensitive JSON is parsed before recursive redaction and capped at 2k chars; malformed JSON returns a marker, never raw text; AOP never inspects args/results.
- **Problems found/corrections:** compile caught wrong caching-wrapper package (`web.filter` → `web.util`). Targeted logging tests passed, but first full suite made `CapturedOutput` empty because a cached Log4j2 context retained an older console stream. Replaced it with test-only appenders attached directly to the two production loggers; unchanged production behavior; full suite passed. Security review additionally redacted member PII and removed raw login identifier/email destination from audit/SMTP logs.
- **Tests/commands executed:** logging targeted **4/4 pass**; first full suite 49/50 (capture seam failure); after test fix final `./mvnw verify` **50/50 pass**.
- **Result:** Log4j2/trace/audit/request-response/AOP baseline complete; frontend remains pending.
- **Commits:** `d009bb2`, `0bcb52a`, `725436b`; docs `4bf37b1`; merge `5099315`.

---

## 2026-08-23 — Session 2 (cont.): Core React frontend

- **Goal:** complete P0-A mentor-demo UI without weakening the backend auth/security model.
- **Files:** frontend design system; auth/session/Axios layer; public/auth/app layouts; catalog/detail; member loans; ADMIN books/members/loans/maintenance; Vitest setup/tests; docs.
- **Important review points:** access token and identity live only in module memory; refresh token is never readable by React; one shared refresh promise prevents a 401 refresh storm; every retried request is marked once; backend remains authorization authority. Catalog state is encoded in URL. All dangerous/admin mutations use explicit feedback/confirmation. ADMIN routes are lazy chunks.
- **UX/a11y:** warm editorial, no gradients/excessive pills; responsive desktop/mobile navigation; skip links, visible labels/focus, 44px targets, reduced-motion, field-local errors, semantic loading/error/empty/success states and keyboard Escape for confirmation dialogs. UI intelligence CLI was unavailable because the installed skill's `scripts` entry is a broken flattened symlink, so the loaded skill rules were applied manually and persisted in `frontend/design-system/MASTER.md`.
- **Problems found/corrections:** Axios 1 test fixture required `AxiosHeaders`; replaced Log4j-style output assumptions with typed frontend tests. Oxlint rejected synchronous prop→state effect in filters; keyed the filter form by URL state instead. Vite warned at 507k main bundle; used `React.lazy`/`Suspense` for ADMIN routes, reducing main chunk to 466k and producing 2–7k route chunks. Fixed dark-background logo contrast, mobile sign-out, admin field error associations, read-only email submission and CSV row-error rendering. First CSV live command failed locally before any request because Windows curl could not open MSYS `/tmp`; reran with a temporary F: path and passed.
- **Tests/commands executed:** final frontend Vitest **20/20 pass**; Oxlint pass; TypeScript + Vite production build pass. Live via Vite proxy: SPA deep route 200; admin login/refresh 200; books/members/loans/config lists 200; Book create 201/update 200/archive 204; CSV import count 1/archive 204; Member create 201/update 200/disable 204; maintenance ON 200 → normal API 503 → control 200 → OFF 200 → normal API 200.
- **Result:** P0-A core frontend complete; P0-B extended auth and final demo documentation remain.
- **Commits:** `251395b`, `3f69feb`, `a05b07b`, `dcc27a7`, `5d03e8b`, `9720fee`, `ead3e9d`, `cf52df6`; docs `8ce75d8`; merge `529a036`.

---

## 2026-08-23 — Session 2 (cont.): Google OAuth P0-B

- **Goal:** add Google login without exposing JWT/provider tokens in URLs and without making credentials a build/test prerequisite.
- **Files:** Liquibase `0011`; `auth/oauth/**`; OAuth DTO/controller/security wiring; frontend provider/callback/session flow; backend/frontend tests; auth/architecture/ERD/decision docs.
- **Important review points:** Spring Security owns OIDC state/nonce/signature validation. Stable Google `sub` is stored in `oauth_identities`; verified email may link an existing account. Callback creates a 256-bit code with 60s TTL; only SHA-256 is stored; SQL conditional update atomically consumes it once. Exchange issues the existing access/refresh model. React removes the query code before exchange. Transient authorization-state session is invalidated after success/failure.
- **Problems found/corrections:** first targeted context failed before assertions because Liquibase used `char(64)` while JPA expected `varchar(64)` for `code_hash`; corrected unpublished `0011`, fresh migration and tests passed. OAuth local-origin review found that proxying the start URL through Vite could place `JSESSIONID` on 5173 while Google callback targets 8080; frontend therefore starts OAuth directly on `VITE_OAUTH_BASE_URL` (8080 in dev). Oxlint flagged immediate error `setState` in callback effect; derived initial error state instead.
- **Tests/commands executed:** OAuth integration **3/3 pass**; final backend `verify` **53/53 pass**; frontend **22/22 pass**, lint/build pass. Dummy-credential live wiring: provider availability true; authorization endpoint 302 to Google with `openid profile email`, transient HttpOnly session cookie, callback URI on backend. Real Google token/consent not claimed because credentials are unavailable.
- **Result:** Google OAuth architecture/domain/UI complete and credential-ready; next P0-B item is email OTP/change email.
- **Commits:** `030477f`, `f8349b9`, `b91486c`, `b7830cc`, `a9d2c4a`; docs commit/merge reported after creation.
