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
- **Commits:** `030477f`, `f8349b9`, `b91486c`, `b7830cc`, `a9d2c4a`; docs `dd2a219`; merge `afb8a6e`.

---

## 2026-08-23 — Session 2 (cont.): Verified change-email OTP

- **Goal:** change registered email only after a robust six-digit verification lifecycle to the new inbox.
- **Files:** OTP repository/service hardening + `OtpAttemptRecorder`; account DTO/service/controller; Account Settings React page/session sync; logging redaction/tests; auth/architecture/decision docs.
- **Important review points:** lookup is latest token bound to authenticated UUID + CHANGE_EMAIL purpose, not global six-digit hash. SHA-256 comparison is constant-time. Wrong attempts use `REQUIRES_NEW`; correct consumption is one conditional SQL update. New email remains only token target until confirmation, then uniqueness is rechecked and account email/verified timestamp update together. JWT `sub` remains UUID, so current session is valid.
- **Problems found/corrections:** recognized that incrementing attempts in the rejected outer transaction would silently roll back (same class of bug as refresh reuse); isolated recorder transaction and proved count=5. Removed `clearAutomatically` from OTP bulk updates because it would detach the loaded user before summary mapping. Targeted test logs exposed raw `newEmail` because redaction matched exact `email`; extended sanitizer to email/phone suffixes and confirmed `[REDACTED]` in final logs.
- **Tests/commands executed:** targeted email change **3/3 pass**; final backend `verify` **56/56 pass**; frontend **23/23 pass**, lint/build pass. Tests cover unchanged-before-confirm, delivered code success, replay, five wrong attempts persisted, correct-after-lockout rejection, resend cooldown and duplicate email. Live through Vite + Mailpit: ADMIN-created verified member → USER login → request 200 → six-digit SMTP capture → confirm 200 → `/me` returned changed email → cleanup disable 204.
- **Result:** Email OTP/change-email P0-B complete; next item is mocked phone OTP.
- **Commits:** `0eee857`, `328c369`, `77302de`, `348d4c8`, `2d72c0d`; docs `78b802f`; merge `0126a79`.

---

## 2026-08-23 — Session 2 (cont.): Mocked phone OTP login

- **Goal/result:** complete P0-B phone OTP without violating no-OTP-log policy. Active registered phones receive a five-minute `PHONE_LOGIN` code through local/test in-memory `FakeSmsSender`; request is enumeration-safe and cooldown-silent; confirmation reuses hashed five-attempt/atomic single-use OTP rules and issues the normal session. Local code retrieval is ADMIN-only; production clearly has no real gateway.
- **Tests/evidence:** targeted phone **3/3 pass**; final backend `verify` **59/59 pass**; frontend **23/23**, lint/build pass. Live Vite flow: request 200, anonymous outbox 401, ADMIN outbox 200, six-digit login USER, replay 400, cleanup disable 204; exact raw code absent from backend log.
- **Correction:** first new component test used an unreliable React Hook Form/user-event timing path and was not committed; production flow had already passed live, frontend's stable suite remained green. No failing test was hidden in reported counts.
- **Commits:** `c01358a`, `ae366b5`, `2100684`, `d8e5aec`; docs commit/merge reported after creation.

---

## 2026-08-24 — Session 3: Facebook OAuth P0-B (last extended-auth item)

- **Goal:** add Facebook login mirroring the Google one-time-code architecture, adapted for Facebook's non-OIDC model.
- **Prompt summary:** "Move on to next task (#5 Facebook OAuth) after Brevo/Google credentials were wired and live-verified."
- **Files:** `auth/oauth/**` (provider enum, conditional client config generalized, success handler dispatch, login service generalized), `OAuthProvidersResponse`, `AuthController`, `application.yml`, frontend `auth-api.ts` + `LoginPage.tsx`; tests in `OAuthFlowIntegrationTest`.
- **Review points:** Facebook is OAuth2+Graph `/me` (no id_token, no `email_verified`) — pinned Graph `v21.0` URLs instead of Spring's ancient v2.8 defaults; Graph email treated as verified (D29); missing email rejected; registration is credential-conditional so local/test boots unchanged.
- **Problems found (by running, not reading):**
  1. First full `verify` failed with an ECJ "Unresolved compilation problem" in `JwtServiceTest` — stale IDE-compiled class in `target/` (VS Code JDT); fixed by `clean`. Not a code bug.
  2. Real javac then caught two genuine handler mistakes the quick `-q compile` had masked incrementally: `OAuth2User` does not extend `java.security.Principal` (param retyped), and a then-redundant `instanceof` pattern (removed). Fixed and re-verified from clean.
- **Commands/tests executed:** targeted OAuth **6/6 pass** (3 Google + 3 new Facebook); final `./mvnw clean verify` **62/62 pass**; frontend **23/23 pass**, oxlint pass, Vite build pass. Live with dummy Facebook credentials: providers `{google:true, facebook:true}`; authorization endpoint 302 → `facebook.com/v21.0/dialog/oauth` with `email,public_profile` scope, state, callback URI; transient HttpOnly handshake cookie set. Real Meta consent not claimed — awaiting app credentials.
- **Result:** P0-B now 4/4 complete. All 35 core+bonus auth requirements done.
- **Commits:** feature commit on `feat/facebook-oauth`; docs commit/merge reported after creation.
- **Live verification (same session):** real Facebook consent completed end-to-end (app needed `email` permission added in the new use-case dashboard + `localhost` in App Domains — console-side only). First attempt 500'd on an **ECJ "Unresolved compilation problems" stub**: VS Code's JDT extension had overwritten Maven-compiled classes in `target/`. Fix: run the packaged JAR (`java -jar`) instead of `mvnw spring-boot:run` while an IDE watches the project. Retry succeeded: `OAUTH_IDENTITY_LINKED provider=FACEBOOK`, account created, session issued.

---

## 2026-08-25 — Session 4: Shelf Aura P2 recommendation feature

- **Goal:** implement P2 Shelf Aura with the agreed choices: enriched book data (page count + tags), public access, top 6 results, and seven moods (`cozy`, `adventurous`, `romantic`, `dark`, `funny`, `thoughtful`, `inspiring`).
- **Files:** Liquibase `0012-book-aura-fields`; `Book`/DTO/mapper/service/CSV extensions; `recommendation/**` engine, query, DTO, controller; public category endpoint; security; React `/aura` page/API/URL state; ADMIN book form fields; tests; README/ERD/requirements/decision docs.
- **Important review points:** legacy seven-column CSV remains valid; extended header appends `pageCount,tags`; missing page counts are neutral rather than guessed; active-only recommendations; score order is deterministic (score desc, title asc, id); reasons and matched tags explain every result; endpoint is public and limited to six.
- **Tests/commands executed:** targeted engine **9/9 pass**; aura integration **4/4 pass**; clean backend `./mvnw verify` **75/75 pass**; frontend typecheck, **27/27 tests**, oxlint, and Vite production build pass. Live local JAR smoke: migration `0012` applied; public categories and aura endpoints returned 200; invalid no-signal request returned 400; admin-created tagged/page-count book received deterministic score/reasons.
- **Result:** P2 #36 Shelf Aura complete; P2 #37 3D bookshelf can reuse the ranked 2D results as its reduced-motion/fallback view.
- **Commit:** reported after review.

### Shelf Aura enhancement follow-up

- **User review:** Philosophy-only recommendations were receiving only the direct theme +4 while multi-signal dark books could reach 8; behavior was deterministic but not sufficiently intuitive.
- **Enhancement:** added bounded alias vocabulary (`Philosophy` → `philosophical`, `reflective`, `meditative`, `essays`, etc.); direct theme category = +4, alias/exact tag signal = +3; kept themes as soft preferences rather than filters.
- **Transparency:** API now returns `breakdown.mood/theme/time/intensity`; reasons include each contribution, including negative time penalties. React cards render the four-part breakdown.
- **Regression proof:** added alias and breakdown assertions; final clean suite is **75/75 backend** and **27/27 frontend**.

---

## 2026-08-25 — Session 5: 3D Shelf Aura (#37)

- **Goal:** add a distinctive 3D bookshelf to `/aura` without changing the deterministic recommendations or weakening the 2D reading path.
- **Files:** `frontend/src/aura/AuraShelf3D.tsx`, `AuraResultView.tsx`, `aura-view.ts`, view-selection/navigation tests, `AuraPage.tsx`, `index.css`; architecture/requirements/decision/wireframe docs.
- **Important review points:** the 3D scene is dependency-free CSS `perspective`/`preserve-3d` in a separate `React.lazy` chunk; it consumes the existing `AuraRecommendation[]`; every upright cover is a keyboard-focusable React Router link to `/books/:bookId`; hover/focus reveals title, author, score, availability, matched tags, and reasons without an extra CTA; list view remains one click away.
- **Fallback behavior:** CSS capability detection, `prefers-reduced-motion`, and a render/lazy-load error boundary select the existing 2D cards. While the chunk loads, cards remain visible; reduced-motion users never load the 3D scene.
- **Tests/commands executed:** frontend **34/34 pass**, Oxlint pass, TypeScript pass, Vite build pass; build emitted a separate `AuraShelf3D` chunk (**5.14 kB**, 1.57 kB gzip). Browser smoke covered desktop/mobile layout, readable upright covers, hover preview, and cover navigation; backend unchanged; clean backend `./mvnw clean verify` also remained green at **75/75**.
- **Result:** P2 #37 implementation complete on `feat/3d-bookshelf`; final branch/merge evidence follows after review.

### Arcane Opus WebGL redesign follow-up

- **User review:** the CSS depth still read as a decorated flat cabinet rather than a professional wow experience. Requested dark-academia magic, true React Three Fiber/Three.js rendering, procedural readable covers, featured-plus-mixed arrangement, mood lighting, pointer motion, particles, stable hover information and a mobile carousel.
- **Rebuild:** replaced all CSS book/shelf geometry with a lazy WebGL chamber: physical cover/page/spine meshes, brass stands, wood architecture, rotating arcane sigil, seeded motes, shadows, cinematic entrance and pointer parallax. Deterministic `CanvasTexture` covers use each title/author and require no external cover service.
- **Interaction:** hovering a mesh advances it and updates a fixed editorial reading panel; clicking opens `/books/:bookId`; arrow keys select and Enter opens; the panel keeps score/breakdown, availability, tags and the top two reasons outside WebGL for readable accessible content. Mobile centers one volume with previous/next carousel controls.
- **Performance/fallback:** `@react-three/fiber` + `three` remain isolated in the existing lazy shelf boundary. The main bundle remains ~495 kB; the WebGL chunk is ~899 kB minified/~239 kB gzip. WebGL2 detection, reduced motion and render/chunk failures still select the existing 2D cards.
- **Verification:** frontend **36/36 tests**, Oxlint, TypeScript and production build pass; clean backend verify remains **75/75**. Browser smoke verified real WebGL2 rendering, procedural covers, desktop chamber, mood copy, fixed panel and the 390px carousel.
- **Decision:** D32 supersedes the visual/technology portion of D31; its lazy loading, direct detail navigation and 2D accessibility fallback remain intact.
