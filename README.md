# BookAura — Library Management System (Fresher Java Assignment 1)

A library management web application: public book catalog, borrow/return with real
inventory control, member management, maintenance mode, and JWT-based auth —
built as a **modular monolith** (Spring Boot + React SPA + PostgreSQL).

> Library, **not** a bookstore: no cart, checkout, payment, shipping, or orders.

## Tech stack

| Layer     | Choice |
|-----------|--------|
| Backend   | Java 17, Spring Boot 3.5.x, Spring Data JPA, Spring Security (JWT), Bean Validation, Liquibase, Log4j2, AOP, Springdoc OpenAPI |
| Frontend  | React 19, TypeScript, Vite, React Router, TanStack Query, Axios, React Hook Form + Zod, Tailwind CSS |
| Database  | PostgreSQL 16 — local: Docker, tests: Testcontainers, demo: Supabase (Postgres only, **no Supabase Auth**) |
| Mail      | `EmailSender` abstraction — local: Mailpit, demo: Brevo SMTP via env vars |
| SMS       | `SmsSender` abstraction — `FakeSmsSender` (console) for local demo only |

## Repository layout

```
backend/    Spring Boot modular monolith (package-by-feature)
frontend/   React SPA
docs/       Design artifacts, decisions, AI worklog
infra/      docker-compose, local infrastructure
```

## Prerequisites

- JDK 17 (this machine: `F:\tools\jdk-17.0.20+8`)
- Docker Desktop (WSL data relocated to `F:\DockerData`)
- Node 22+
- No global Maven needed — use the committed Maven Wrapper (`mvnw`)

## Quickstart (local)

```bash
# 1. Infrastructure (PostgreSQL 16 + Mailpit on http://localhost:8025)
docker compose -f infra/docker-compose.yml up -d

# 2. Backend (local profile; Liquibase migrates an empty DB automatically)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# API: http://localhost:8080  |  OpenAPI UI: http://localhost:8080/swagger-ui.html

# 3. Frontend
cd frontend
npm install
npm run dev
# SPA: http://localhost:5173 (Vite proxies /api to localhost:8080)

# Frontend quality gate
npm test
npm run lint
npm run build
```

Local demo account (seeded only in `local` profile): `admin / admin`.

## Implemented API slices

- Auth: `/api/auth/register`, `/verify-email`, `/login`, `/refresh`, `/logout`, `/me`;
  optional Google OIDC starts at `/oauth2/authorization/google` and exchanges at `/api/auth/oauth/exchange`.
- Public catalog: `GET /api/books`, `GET /api/books/{id}`
- ADMIN books: CRUD/search under `/api/admin/books`; CSV: `POST /api/admin/books/import`
- Loans: `POST /api/loans`, `POST /api/loans/{id}/return`, `/active`, `/history`;
  ADMIN management under `/api/admin/loans`.
- ADMIN members: CRUD/search under `/api/admin/members`; date range format is strict `yyyy/MM/d`;
  search includes name, email/phone, DoB, borrowed book title, status, role and verification.
- ADMIN operations: `GET /api/admin/system-config`, `PUT /api/admin/system-config/maintenance`;
  normal APIs return 503 while health/control remain available.
- Multi-sort example: `?sort=publicationYear:desc,title:asc`; maximum page size is 10.

CSV header is exact; author/category lists use `|`:

```csv
title,isbn,authors,categories,publicationYear,totalQuantity,description
Clean Code,9780132350884,Robert C. Martin,Programming|Software Engineering,2008,3,A handbook of agile software craftsmanship
```

Import is all-or-nothing. File must be `.csv` and **strictly below 5 MiB**; validation
errors are returned by row (for example `row[3].isbn`).

Observability: every response carries `X-Trace-Id`; Log4j2 writes console + rolling file logs;
HTTP bodies are bounded/redacted (auth bodies suppressed), and `@LogOperation` AOP records
service outcome/duration without arguments or return values.

## Implemented frontend journeys

- Public editorial landing page, URL-backed catalog filters, availability and book detail.
- Registration, email-link verification and login; access token is memory-only, refresh cookie is HttpOnly.
- Member borrow, active loans, due/overdue state, confirmed return and permanent history.
- ADMIN route-guarded workspaces: Book CRUD/archive/CSV, Member search/CRUD/disable,
  loan oversight/admin return and maintenance control.
- Responsive app shell, accessible forms/dialogs, error/empty/loading states and lazy ADMIN routes.

## Environment variables

See `.env.example`. Real secrets go in a local `.env` file — **never committed**
(`.gitignore` enforces this). Demo email uses Brevo SMTP. Google OAuth is enabled only when both `GOOGLE_CLIENT_ID` and
`GOOGLE_CLIENT_SECRET` are non-blank; local callback: `http://localhost:8080/login/oauth2/code/google`.

## Docs

- [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) — frozen scope & priorities
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — modules, layers, deployment
- [docs/ERD.md](docs/ERD.md) — data model
- [docs/AUTH_FLOW.md](docs/AUTH_FLOW.md) — registration, login, refresh rotation, logout
- [docs/DECISIONS.md](docs/DECISIONS.md) — all recorded decisions (ADR-lite)
- [docs/AI_WORKLOG.md](docs/AI_WORKLOG.md) — AI-assisted development evidence
