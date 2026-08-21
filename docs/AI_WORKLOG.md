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
