# Changelog

All notable changes to this project are documented here.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/); this
project uses [Semantic Versioning](https://semver.org/).

## v1.0.0

First production release. A full-stack psychometric assessment platform
(Spring Boot API + React SPA + MySQL) with three roles: SUPER_ADMIN > ADMIN
(school/teacher) > USER (student).

### Added
- Role-based platform: super-admin-owned classes/subjects/quizzes/questions;
  schools manage their students; students take assessments.
- Psychometric scoring engine with per-dimension results.
- The Mentalist multi-page PDF report generation.
- AI-assisted report summaries via an OpenAI-compatible provider, configured at
  runtime by a super admin (key stored in the DB, never exposed raw).
- Public marketing landing page at `/`; login moved to `/login`.
- Flyway-owned schema (migrations V1–V23); Hibernate runs in `validate` mode.
- `GET /health` endpoint returning `{"status":"ok","version":"1.0.0"}`.
- Production Spring profile (`prod`) with fail-fast configuration validation.
- Global exception handler: intentional errors preserved, unexpected ones logged
  and returned as an opaque 500.
- Docker packaging: backend (multi-stage Maven → JRE), frontend (build → nginx
  with `/api` reverse-proxy and SPA routing), and a `docker-compose.yml` for the
  full stack (MySQL + backend + frontend) with health checks.
- `.env.example` templates (repo root for Compose, and backend-specific).

### Changed
- Backend version `0.0.1-SNAPSHOT` → `1.0.0`.
- Auth filter diagnostics moved from `System.out.println` to SLF4J `debug`.
- Production logging levels (INFO app, WARN framework); SQL logging disabled in prod.
- Graceful shutdown enabled for clean container SIGTERM handling.

### Fixed
- Removed development debug logging (23 `console.log`) that leaked response data
  to the browser console.
- Removed dead code (unused vars, dead click handlers) including a latent
  null-dereference in the header.

### Known Limitations
- `JWT_SECRET` still has a development fallback under the default (non-`prod`)
  profile; only the `prod` profile enforces a strong secret. Always run with
  `SPRING_PROFILES_ACTIVE=prod` in production.
- AI provider API key is stored as plaintext in the database (masked in API
  responses). Encryption at rest is not yet implemented.
- TLS is expected to be terminated by an external reverse proxy (nginx/Traefik/
  Dockploy); the bundled nginx serves plain HTTP inside the network.
- No email or object-storage integration.
- The default CRA `App.test.js` is a stale sample and is not a real test.
