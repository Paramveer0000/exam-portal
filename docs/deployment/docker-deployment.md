# Docker Deployment Guide

Covers the containerised production stack: architecture, networking, volumes,
build, deploy, update, rollback, backup, and troubleshooting. Pairs with
[docs/release/phase-3-docker-readiness.md](../release/phase-3-docker-readiness.md)
(the audit/scoring) and the repo root [README.md](../../README.md).

---

## 1. Architecture Overview

```
                     ┌─────────────────────────────────────────┐
  Internet ──HTTPS──▶│ Dockploy reverse proxy (Traefik) / VPS  │
                     │ nginx / your own TLS terminator          │
                     └───────────────────┬───────────────────────┘
                                          │ HTTP, port ${FRONTEND_PORT} (default 8080)
                     ┌────────────────────▼────────────────────┐
                     │  frontend  (nginx-unprivileged:8080)     │
                     │  serves the CRA static bundle            │
                     │  proxies /api/*, /health -> backend      │
                     └────────────────────┬──────────────────────┘
                                          │ internal network only
                     ┌────────────────────▼────────────────────┐
                     │  backend   (Spring Boot, port 8081)      │
                     │  profile=prod, JWT auth, Flyway migrate  │
                     └────────────────────┬──────────────────────┘
                                          │ internal network only
                     ┌────────────────────▼────────────────────┐
                     │  db        (MySQL 8, port 3306)          │
                     │  volume: db_data                         │
                     └───────────────────────────────────────────┘
```

Three services, one Compose project (`exam-portal`), one internal bridge
network. Only `frontend` publishes a port; `backend` and `db` are reachable
solely from other containers on the `internal` network.

## 2. Container Descriptions

| Service | Image | Built from | Runs as | Purpose |
|---|---|---|---|---|
| `db` | `mysql:8.0` | (official image) | mysql (image default) | Schema + data, Flyway-migrated by the backend on boot. |
| `backend` | built | `exam-portal-backend/Dockerfile` (multi-stage: `maven:3.9-eclipse-temurin-17` → `eclipse-temurin:17-jre-alpine`) | non-root `app` user | REST API, JWT auth, PDF report generation, `/health`. |
| `frontend` | built | `exam-portal-frontend/Dockerfile` (multi-stage: `node:18-alpine` → `nginxinc/nginx-unprivileged:1.27-alpine`) | non-root nginx (image default) | Serves the React SPA; reverse-proxies `/api/*` and `/health` to `backend`. |

All three: `restart: unless-stopped`, a `healthcheck`, `container_name` set,
and `deploy.resources.limits` (see §11).

## 3. Networking

- Single explicit bridge network, `internal` (Compose project name `exam-portal`,
  so containers are reachable at `exam-portal-<service>` / short names `db`,
  `backend`, `frontend` for DNS within the network).
- `db` and `backend` use `expose` only — no host port, unreachable outside
  the Compose network.
- `frontend` is the only service with a `ports:` mapping
  (`${FRONTEND_PORT:-8080}:8080`) — the single ingress point.
- **Reverse proxy decision:** no nginx/Traefik container is added *in front of*
  `frontend` in this repo. Dockploy already runs its own Traefik-based reverse
  proxy and TLS termination in front of whatever port a Dockploy app publishes
  — stacking a second internet-facing proxy here would be redundant
  complexity for no benefit. If you deploy this Compose file on a bare VPS
  *without* Dockploy, put your own TLS-terminating proxy (nginx/Caddy/Traefik)
  in front of `${FRONTEND_PORT}` — the `frontend` container itself only ever
  speaks plain HTTP.

## 4. Volumes

| Volume | Mounted at | Contains | Persistence |
|---|---|---|---|
| `db_data` | `db:/var/lib/mysql` | MySQL data files | Must survive redeploys — never `docker compose down -v` in production. |
| `reports_data` | `backend:/app/data/mentalist-reports` | Generated Mentalist PDF reports | Same — user-facing generated content. |

Both are named (not anonymous) Docker volumes, so `docker compose up -d
--build` (no `-v`) never touches them.

## 5. Environment Variables

See the annotated template: [`.env.example`](../../.env.example) (repo root,
consumed by `docker-compose.yml`). Summary:

| Variable | Required | Notes |
|---|---|---|
| `DB_NAME` | no | default `exam-portal` |
| `DB_PASSWORD` | **yes** | Compose fails fast (`:?`) if unset |
| `JWT_SECRET` | **yes** | Compose fails fast (`:?`); backend `EnvironmentValidator` also rejects it if <32 chars or the built-in dev value |
| `SUPERADMIN_USERNAME/PASSWORD/FIRST_NAME/LAST_NAME/PHONE` | no | first super-admin seed; blank password skips seeding |
| `FRONTEND_PORT` | no | default `8080` |
| `TZ` | no | default `UTC`, applied to all three containers |

No secret has a real default baked into `docker-compose.yml` — `DB_PASSWORD`
and `JWT_SECRET` use Compose's `${VAR:?error message}` syntax, which aborts
`docker compose up` with that message if the variable is missing.

## 6. Build Process

```bash
cp .env.example .env        # fill in DB_PASSWORD, JWT_SECRET at minimum
docker compose build        # or: docker compose up -d --build
```

- **Backend:** multi-stage — `mvn dependency:go-offline` on `pom.xml` alone
  (cached layer, only invalidated when `pom.xml` changes), then `mvn clean
  package -DskipTests` once `src/` is added. Final image copies only the fat
  jar into a JRE (not JDK) Alpine base.
- **Frontend:** `npm ci --legacy-peer-deps` (cached on the lockfile), then
  `npm run build`. Final image copies only `build/` into an nginx image —
  none of `node_modules`, source, or the Node toolchain ship in the runtime
  image.

## 7. Deployment Steps

```bash
# On the VPS (Dockploy or plain Docker Compose):
git clone <repo> && cd <repo>
cp .env.example .env
$EDITOR .env                        # set DB_PASSWORD, JWT_SECRET, SUPERADMIN_PASSWORD, TZ
docker compose up -d --build
docker compose ps                   # all three should become "healthy"
curl http://localhost:${FRONTEND_PORT:-8080}/health
```

Under Dockploy: point a Compose application at this repo/branch; Dockploy
builds both images from their Dockerfiles, runs `docker compose up -d`
equivalent, and fronts the published `frontend` port with its own proxy/TLS.

## 8. Update Procedure

```
Developer
  │  git commit
  ▼
GitHub (push to main / release branch)
  │
  ▼
Dockploy pulls the new commit
  │
  ▼
docker compose build   (rebuilds only changed image layers)
  │
  ▼
Flyway migrations run automatically on backend startup
  │
  ▼
Compose healthchecks gate the rollout (backend waits on db, frontend waits on backend)
  │
  ▼
GET /health returns 200  →  Production Ready
```

Manual equivalent:

```bash
git pull
docker compose up -d --build
docker compose logs -f backend      # watch for "Started ExamPortalBackendApplication"
                                     # and "Successfully applied N migrations"
```

Because Hibernate runs `ddl-auto=validate`, any code change that needs a
schema change **must** ship its Flyway migration (`V<n>__description.sql`) in
the same commit/release — the app will fail to start otherwise (a safe,
loud failure, not silent drift).

## 9. Rollback Procedure

```bash
git log --oneline -5                # find the last known-good commit/tag
git checkout <previous-tag-or-sha>
docker compose up -d --build        # rebuilds images from that commit
```

- **Database:** Flyway does not auto-rollback. If the failed release included
  a forward-only migration, rolling back code without a matching down-migration
  can leave the schema ahead of the code. For a destructive/breaking migration,
  restore the `db_data` volume from the pre-deploy backup (§10) rather than
  relying on code rollback alone.
- **Fast mitigation without a rebuild:** `docker compose restart backend` /
  `frontend` if the issue is transient (bad env value, OOM) rather than a bad
  release.

## 10. Backup Strategy

```bash
# Database (logical dump)
docker compose exec db sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" exam-portal' \
  > backups/db-$(date +%F).sql

# Generated PDF reports (reports_data volume)
docker run --rm -v exam-portal_reports_data:/data -v "$(pwd)/backups:/out" alpine \
  tar czf /out/reports-$(date +%F).tar.gz -C /data .

# Configuration (never commit the real .env; back it up separately, encrypted)
cp .env backups/env-$(date +%F).bak
```

**Restore:**

```bash
docker compose exec -T db sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" exam-portal' \
  < backups/db-YYYY-MM-DD.sql

docker run --rm -v exam-portal_reports_data:/data -v "$(pwd)/backups:/in" alpine \
  sh -c "cd /data && tar xzf /in/reports-YYYY-MM-DD.tar.gz"
```

Schedule the dump/tar commands via cron (or Dockploy's scheduled jobs) daily,
retain at least 7 days on-box and ship a copy off-VPS (S3-compatible storage,
another host, etc.) — a single-VPS backup that only lives on that VPS is not
a backup against VPS loss.

## 11. Health Checks

| Service | Check | Command |
|---|---|---|
| `backend` | `GET /health` → `{"status":"ok","version":"1.0.0"}` | `wget -qO- http://localhost:8081/health` (in-container) |
| `frontend` | root page responds | `wget -qO- http://localhost:8080/` (in-container) |
| `db` | MySQL accepts connections | `mysqladmin ping` |

All three are wired into `docker-compose.yml` `healthcheck:` blocks and gate
`depends_on: condition: service_healthy`, so `backend` won't start serving
until `db` is healthy, and `frontend` won't start until `backend` is healthy.
`/health` is a **liveness** check only (no DB round-trip) — see the readiness
report for the recommended follow-up (`/health/ready`).

## 12. Troubleshooting

| Symptom | Cause / Fix |
|---|---|
| `docker compose up` aborts immediately with "set DB_PASSWORD in .env" / "set JWT_SECRET in .env" | Required var missing from `.env` — Compose's `:?` guard caught it before any container started. |
| `backend` container restarts in a loop, log shows `FATAL: production configuration error` | `JWT_SECRET` too short or still the dev default — see `EnvironmentValidator`. Set a real secret. |
| `backend` never becomes healthy, `db` shows `unhealthy` | Check `docker compose logs db` — usually a bad `DB_PASSWORD` or the volume from a previous, different password. |
| `frontend` up, but `/api/*` calls fail | `backend` isn't healthy yet, or `depends_on` conditions removed — check `docker compose ps`. |
| Every authenticated API call 401 after a redeploy | `JWT_SECRET` changed between deploys — existing tokens are invalidated by design; users must re-login. |
| Migration fails on startup: "checksum mismatch" | An already-applied Flyway migration file was edited after being applied — never edit a shipped migration; add a new one instead. |
