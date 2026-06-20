# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project

`webiados-cotizaciones-api` — Spring Boot 3.x REST API that powers the Webiados quoting flow. Clients submit quote requests through a public form (Angular frontend at `cotiza.webiados.com`); admins review, price, and manage quotes through a private panel. Deployed on Railway with a PostgreSQL plugin.

## Commands

- `./mvnw spring-boot:run` — start dev server (requires local Postgres or `.env` pointing at Railway).
- `./mvnw test` — run unit + integration tests.
- `./mvnw package -DskipTests` — build the fat JAR.
- `docker build -t webiados-cotizaciones-api .` — build the Docker image (Dockerfile at root).
- Copy `.env.example` → `.env` and fill in values before running locally.

## Architecture

### Domain
- `Quote` — a client's quote request. Has status (`PENDING`, `REVIEWED`, `SENT`, `ACCEPTED`, `REJECTED`). Belongs to a client (email + phone) and contains `QuoteOption` line-items.
- `QuoteOption` — a selectable service/product within a quote (name, price, quantity).
- `Selection` — a pre-defined service card the client picks from the public form (`SelectionKind`: WEB, SOFTWARE, ECOMMERCE, etc.).
- `AdminUser` — back-office user with hashed password and JWT auth.

### Layers
```
web/          → REST controllers (AdminQuoteController, ClientQuoteController, AdminAuthController)
service/      → business logic (QuoteService, SelectionService, AuthService, EmailService)
repo/         → Spring Data JPA repositories
domain/       → JPA entities + enums
dto/          → request/response DTOs (admin/, client/ sub-packages)
config/       → CORS, Security, AppProperties, JwtProperties
security/     → JwtAuthFilter, JwtService, RateLimiter
```

### Database
- PostgreSQL (Railway). Schema managed by Flyway migrations in `src/main/resources/db/migration/`.
- `V1__init.sql` — base schema (quotes, quote_options, selections, admin_users).
- `V2__add_clave_texto.sql` — adds `clave_texto` to admin recovery.
- `V3__add_landing_fields.sql` — adds `titulo`, `mensaje`, `imagenes` to quotes for branded landing pages.

### Auth
- Admins authenticate via `POST /api/admin/auth/login` → JWT in response body.
- JWT is sent as `Authorization: Bearer <token>` on every protected request.
- `RateLimiter` blocks brute-force on the unlock endpoint.

### API surface

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/quotes` | public | Client submits quote request |
| GET | `/api/quotes/{code}` | public (code) | Client views their quote |
| GET | `/api/admin/quotes` | admin | List all quotes |
| GET | `/api/admin/quotes/{id}` | admin | Quote detail |
| PUT | `/api/admin/quotes/{id}` | admin | Update quote (price, status, options, landing fields) |
| POST | `/api/admin/auth/login` | — | Admin login |
| POST | `/api/admin/auth/unlock` | — | Admin unlock via recovery key |
| GET | `/api/selections` | public | List selectable services |

### Email
`EmailService` sends transactional mail via SMTP (Resend recommended). Triggered on new quote submission and on status change to `SENT`.

## Conventions

- Spring Boot 3 / Java 21. Records for DTOs where immutability makes sense.
- `application.yml` reads **all secrets from env vars** — never hardcode credentials.
- Flyway migration files follow `V{n}__{description}.sql` naming; never edit existing migrations.
- CORS allowed origins are configured via `CORS_ALLOWED_ORIGINS` env var (comma-separated).
- The frontend counterpart lives at `github.com/Pipedsl/webiados` (Angular 21, `cotiza.webiados.com` subdomain).

## Environment variables

See `.env.example` for the full list. Key vars:

| Var | Notes |
|-----|-------|
| `DATABASE_URL` | Injected by Railway Postgres plugin |
| `JWT_SECRET` | Long random string (≥64 chars) |
| `ADMIN_BOOTSTRAP_EMAIL/PASSWORD` | Seeds first admin on cold start |
| `MAIL_*` | SMTP config (Resend or Brevo) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins |
