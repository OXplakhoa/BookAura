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
- **Commits:** `f74c141`, `32b012a`, `9979f60`; docs commit and merge reported after creation.
