# The Mentalist — Psychometric Assessment Platform

Full-stack platform for running psychometric assessments and generating student
reports. Spring Boot REST API + React SPA + MySQL.

Three roles, hierarchical: **SUPER_ADMIN** (platform owner) > **ADMIN**
(school/teacher) > **USER** (student). Super admins own the assessment content
(classes, subjects, quizzes, questions); schools manage their students; students
take assessments and receive psychometric profiles and PDF reports.

- **Version:** 1.0.0 — see [CHANGELOG.md](CHANGELOG.md)

---

## Requirements

| Tool | Version | Notes |
|---|---|---|
| JDK | **17** | Not 25 — Lombok is pinned for 17. |
| Node.js | 18+ | For the frontend build. |
| MySQL | 8.0 | Local dev or the Compose `db` service. |
| Docker + Compose | recent | For the containerised production stack. |
| Maven | wrapper included | Use `./mvnw`; no system Maven needed. |

---

## Folder Structure

```
.
├── exam-portal-backend/      Spring Boot 2.7 API (Java 17, port 8081)
│   ├── src/main/java/com/project/examportalbackend/
│   │   ├── controllers/      thin REST controllers (incl. HealthController)
│   │   ├── services/         business logic + ownership checks
│   │   ├── repository/       Spring Data JPA repositories
│   │   ├── models/ dto/      entities and DTOs
│   │   ├── configurations/   security, JWT, prod validation, error handling
│   │   └── bootstrap/        first super-admin seeding
│   ├── src/main/resources/
│   │   ├── application.properties         base config
│   │   ├── application-prod.properties     production overrides
│   │   └── db/migration/     Flyway migrations V1–V23
│   ├── Dockerfile
│   └── .env.example          backend env vars (non-Docker runs)
├── exam-portal-frontend/     React 18 SPA (CRA, Bootstrap, Redux-thunk)
│   ├── src/                  pages, components, services, redux slices
│   ├── Dockerfile
│   └── nginx.conf            prod static serving + /api reverse proxy
├── seed/psychometric/        question banks + seeding/validation scripts
├── docs/release/             cleanup + production-readiness reports
├── docker-compose.yml        full production stack
├── .env.example              Compose environment template
├── VERSION                   1.0.0
└── CHANGELOG.md
```

---

## Environment Variables

Copy the template and fill it in (the real `.env` is git-ignored):

```bash
cp .env.example .env
```

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `DB_NAME` | no | `exam-portal` | MySQL database name (auto-created). |
| `DB_PASSWORD` | **yes** | — | MySQL root password. |
| `JWT_SECRET` | **yes (prod)** | dev fallback outside prod | JWT signing secret. In `prod`: must be set, ≥32 chars, not the dev default. |
| `SUPERADMIN_USERNAME` | no | `superadmin` | First super admin username. |
| `SUPERADMIN_PASSWORD` | no | — (blank = skip seeding) | Seeds the first super admin on first boot. |
| `SUPERADMIN_FIRST_NAME` / `_LAST_NAME` / `_PHONE` | no | `Super` / `Admin` / `0000000000` | Super admin profile fields. |
| `FRONTEND_PORT` | no | `8080` | Host port the frontend is published on. |
| `DB_URL` / `DB_USERNAME` | no | local defaults | Full JDBC URL / user (Compose sets these for you). |
| `MENTALIST_REPORTS_DIR` | no | `./data/mentalist-reports` | Where generated PDFs are written. |
| `SPRING_PROFILES_ACTIVE` | prod | — | Set to `prod` for production hardening. |

The **AI provider API key is not an environment variable** — a super admin sets
it at runtime (Super Admin → AI Settings); it is stored in the DB and never
returned raw. No email/S3 integration exists, so no `SMTP_*`/`S3_*` vars.

---

## Running Locally (without Docker)

Start order matters: **MySQL → backend → frontend.**

```bash
# 1. MySQL must be up on :3306 with credentials matching your env.

# 2. Backend (port 8081)
cd exam-portal-backend
export JWT_SECRET=local-dev-secret          # any non-empty value in dev
export SUPERADMIN_PASSWORD=change-me         # only if you want the super admin seeded
./mvnw spring-boot:run

# 3. Frontend (port 3000, proxies /api -> 127.0.0.1:8081)
cd exam-portal-frontend
npm install --legacy-peer-deps               # required (peer-dep conflicts otherwise)
npm start
```

Open http://localhost:3000. Flyway applies pending migrations on backend startup.

---

## Docker Setup

Build and run the whole stack (MySQL + backend + frontend):

```bash
cp .env.example .env        # fill in DB_PASSWORD and JWT_SECRET at minimum
docker compose up -d --build
```

- Frontend: http://localhost:${FRONTEND_PORT:-8080}
- Backend health: proxied at `/health`, or directly on the backend container `:8081/health`
- The backend runs with `SPRING_PROFILES_ACTIVE=prod` (fail-fast config validation).

Stop / view logs:

```bash
docker compose down                 # keep volumes (data persists)
docker compose logs -f backend
```

---

## Production Deployment (Hostinger VPS + Dockploy)

1. Provision the VPS with Docker + Docker Compose.
2. Clone the repo, `cp .env.example .env`, set strong `DB_PASSWORD` and
   `JWT_SECRET` (`openssl rand -base64 48`), and a `SUPERADMIN_PASSWORD`.
3. In **Dockploy**, point a Compose app at this repo; it builds the two images
   and runs the stack. Health checks are defined per service.
4. Terminate TLS at an external reverse proxy (Dockploy/Traefik/nginx) in front
   of the `frontend` service — the bundled nginx serves plain HTTP internally.
5. Persist volumes `db_data` (MySQL) and `reports_data` (generated PDFs).

Do not expose MySQL to the public internet — it is only reachable on the Compose
network.

---

## Database Migration

- Schema is owned by **Flyway** (`exam-portal-backend/src/main/resources/db/migration/V1..V23`).
  Hibernate runs `ddl-auto=validate` and never alters the schema.
- Migrations apply automatically on backend startup; watch the log for
  `Successfully applied` / `now at version`.
- To add a schema change, add a new `V<n>__description.sql` file — never edit an
  applied migration and never change schema via entity annotations alone.

---

## Backup Process

```bash
# MySQL logical backup (from the host)
docker compose exec db sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" exam-portal' > backup-$(date +%F).sql

# Generated PDF reports live in the reports_data volume
docker run --rm -v $(pwd):/out -v examportalmaster_reports_data:/data alpine \
  tar czf /out/reports-$(date +%F).tar.gz -C /data .
```

Restore the database with:

```bash
docker compose exec -T db sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" exam-portal' < backup-YYYY-MM-DD.sql
```

---

## Update Process

```bash
git pull
docker compose up -d --build         # rebuilds changed images, applies new Flyway migrations on boot
docker compose logs -f backend       # confirm "Started ExamPortalBackendApplication" and migrations
```

Because Hibernate is `validate`-only, a code update that expects a schema change
must ship the matching Flyway migration in the same release.

---

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| Backend exits immediately in prod with "production configuration error" | `JWT_SECRET` missing, too short, or the dev default. Set a strong secret. |
| `Flyway ... Unable to obtain connection` | MySQL down or wrong `DB_PASSWORD`. |
| `Schema validation: missing table/column` | An entity changed without a migration — add one. |
| Every authed call returns 401/403 | `JWT_SECRET` differs from the one that signed the token — re-login. |
| Frontend loads but `/api` calls 404 | nginx/proxy not routing `/api` to the backend, or backend not healthy yet. |
| `npm install` peer-dep errors | Use `npm install --legacy-peer-deps`. |

---

## Health Check

```
GET /health  ->  {"status":"ok","version":"1.0.0"}
```

Public, no auth, no DB work — safe for orchestrator liveness probes.
