# Phase 3 — Docker, Dockploy & Production Deployment Readiness

**Target release:** v1.0.0
**Date:** 2026-07-29
**Deployment targets:** GitHub · Docker · Dockploy · Hostinger VPS
**Builds on:** Phase 1 (cleanup) and Phase 2 (env/release prep, initial Docker artifacts). This phase
audits, hardens, and validates that Docker setup rather than creating it from scratch.

---

## 1. Docker Audit Summary

All three required artifacts existed from Phase 2 and were re-audited against a stricter production
checklist (small images, layer caching, non-root, health checks, resource limits, network isolation):

| Artifact | Status |
|---|---|
| `exam-portal-backend/Dockerfile` | Present, hardened this phase |
| `exam-portal-frontend/Dockerfile` | Present, hardened this phase |
| `docker-compose.yml` | Present, hardened this phase |
| `.dockerignore` ×2 | Present, extended this phase |

No Redis, message queue, or worker process exists anywhere in the codebase (confirmed by repo-wide
search) — **Redis section is N/A**, stated explicitly rather than skipped.

## 2. Dockerfile Improvements (this phase)

### Backend (`exam-portal-backend/Dockerfile`)

| Change | Reason |
|---|---|
| Multi-stage `maven:3.9-eclipse-temurin-17` → `eclipse-temurin:17-jre-alpine` | Already in place from Phase 2 — JRE (not JDK) in the final image; Maven toolchain never ships. |
| Merged user-creation + directory-creation + chown into **one `RUN`** | Fewer image layers. |
| `COPY --from=build --chown=app:app` | Jar lands already owned by the runtime user — no separate chown layer needed for it. |
| Added `JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"`, `ENTRYPOINT exec java $JAVA_OPTS -jar app.jar` | JVM sizes its heap as a percentage of the **container's** cgroup memory limit (set in Compose, §11) instead of a fixed `-Xmx` that would need updating if the limit changes. `ExitOnOutOfMemoryError` fails fast + lets `restart: unless-stopped` recover instead of a wedged JVM. `exec` keeps `java` as PID 1 so `SIGTERM` reaches it directly (required for `server.shutdown=graceful` to work). |
| `HEALTHCHECK` (unchanged from Phase 2) | Already present, verified still correct. |
| Non-root `app` user (unchanged from Phase 2) | Already present. |

### Frontend (`exam-portal-frontend/Dockerfile`)

| Change | Reason |
|---|---|
| `ENV NODE_ENV=production` added explicitly in the build stage | CRA's `npm run build` sets this itself, but making it explicit means any other tooling invoked in that stage also sees it — belt-and-braces per the brief's checklist. |
| `npm ci --legacy-peer-deps && npm cache clean --force` merged into one `RUN` | One fewer layer; npm's cache never persists into an image layer. |
| **Base image swapped:** `nginx:1.27-alpine` → `nginxinc/nginx-unprivileged:1.27-alpine` | The previous image ran nginx's **master** process as root (workers as non-root — standard but not fully non-root). The unprivileged image runs the entire process, master included, as a non-root user. This is why the container now listens on **8080** instead of 80 (non-root can't bind <1024). |
| `EXPOSE 8080` (was 80); `nginx.conf` `listen 8080` | Matches the unprivileged image. `docker-compose.yml` port mapping updated to `${FRONTEND_PORT:-8080}:8080`. |
| `HEALTHCHECK` target updated to `:8080` | Matches the new listen port. |

## 3. `.dockerignore`

Extended both files this phase to also exclude: `.github`, `logs/`, `tmp/`, `cache/`/`.cache/`,
`docs/` (backend only — docs aren't needed in the build context), and the sole test file
(`src/**/*.test.js`, frontend). Already present from Phase 2: `node_modules`, `.git`, `.idea`,
`.vscode`, `.env*`, `*.log`, `coverage/`, `build/`/`target/`.

## 4. Docker Compose Improvements

| Area | Before (Phase 2) | After (this phase) |
|---|---|---|
| Container names | implicit | explicit `container_name` on all 3 services |
| Network | Compose default network | explicit named `internal` bridge network; only `frontend` publishes a port |
| Timezone | not set | `TZ` env (default `UTC`) on all 3 services |
| Resource limits | none | `deploy.resources.limits` per service (cpus/memory — see §11) |
| Logging | Docker default (unbounded `json-file`) | explicit `json-file` driver, `max-size: 10m`, `max-file: 3` (anchor `x-logging`, reused by all 3) |
| Secrets | already env-based, `:?` required-var guards | unchanged — confirmed still correct |
| Health-gated startup | already `condition: service_healthy` | unchanged — confirmed still correct |
| Frontend port | `8080:80` | `8080:8080` (matches the unprivileged nginx image) |
| `name:` (project name) | implicit from directory | explicit `name: exam-portal`, so volume/network names are stable regardless of the clone directory name |

**Validated:** `docker compose config` (client-side syntax + interpolation check, run with a temporary
throwaway `.env` — deleted immediately after) renders successfully: all environment variables resolve,
resource limits parse, healthchecks and `depends_on` conditions are well-formed. Output confirmed
correct interpolation of `DB_URL`, resource limits (`1073741824` bytes = 1024M, etc.), and the
`internal` network on all three services.

## 5. Production Environment

- `SPRING_PROFILES_ACTIVE=prod` baked into the backend image's `ENV` (Phase 2) and also set explicitly
  in Compose (belt-and-braces).
- No Node.js in the runtime path (React is a static build served by nginx), so there is no server-side
  `NODE_ENV` to set at runtime; the **build-time** `NODE_ENV=production` is set explicitly (§2).
- `TZ` applied to all containers.
- `container_name` set on all three (predictable names for `docker logs <name>`, `docker exec`, etc.).
- `restart: unless-stopped` on all three (already present, confirmed).
- Network isolation: `db` and `backend` have no `ports:` mapping — unreachable from outside Docker.
- Volumes (`db_data`, `reports_data`) are named, not anonymous — survive `docker compose up -d --build`.
- No development-only configuration ships: `spring-boot-devtools` is `optional`/`runtime`-scoped and
  inert in the packaged jar; CRA's `setupProxy.js` only runs under `react-scripts start`, never in the
  production `build/` output the frontend image actually serves.

## 6. Database Readiness

- **Persistent storage:** named volume `db_data` on `/var/lib/mysql`.
- **Automatic migrations:** Flyway runs on backend startup (V1–V23 already applied in dev; new
  migrations apply automatically on next boot). Documented in the README and the deployment guide.
- **Safe startup order:** `backend` has `depends_on: db: condition: service_healthy` — it does not
  even attempt to connect until MySQL's own healthcheck (`mysqladmin ping`) passes. `frontend`
  similarly waits on `backend`'s `/health`.
- **Backup/restore:** documented with runnable commands in
  [docs/deployment/docker-deployment.md §10](../deployment/docker-deployment.md#10-backup-strategy).
- No data was deleted or modified as part of this phase.

## 7. Redis

**Not applicable.** Confirmed by repository-wide search (`pom.xml`, `application*.properties`,
`docker-compose.yml`, frontend `package.json`) — no Redis client, dependency, or configuration exists
anywhere in this codebase. No caching layer, session store, or queue is used; sessions are stateless
JWT (`SessionCreationPolicy.STATELESS`).

## 8. Reverse Proxy

**Recommendation: use Dockploy's built-in reverse proxy (Traefik-based); do not add another
nginx/Traefik/Caddy container in front of the stack.**

Reasoning:
- Dockploy already provides TLS termination, routing, and Let's Encrypt integration for whatever port
  a Dockploy-managed Compose app publishes.
- The `frontend` container's nginx already does the *internal* job a reverse proxy would otherwise
  need to do anyway (serve the SPA, proxy `/api/*` to `backend`) — it is not a second general-purpose
  proxy, it's the app's own static-file + routing layer.
- Adding a second internet-facing proxy (e.g., a standalone Traefik container) in front of Dockploy's
  own proxy would double TLS termination, double the failure surface, and add nothing — Dockploy would
  just proxy to the second proxy.
- **If deployed outside Dockploy** (plain Hostinger VPS with just Docker Compose, no Dockploy), an
  external TLS-terminating proxy (nginx, Caddy, or Traefik) is still required in front of
  `${FRONTEND_PORT}` — this is documented in the deployment guide as the non-Dockploy path.

## 9. HTTPS Readiness

| Item | Status |
|---|---|
| Proxy compatibility | `nginx.conf` already forwards `X-Forwarded-For` / `X-Forwarded-Proto` / `Host` to the backend on `/api/*`. |
| Trusted proxy settings (backend) | **Added this phase:** `server.forward-headers-strategy=framework` in `application-prod.properties`, so Spring reads `X-Forwarded-*` correctly. |
| Secure cookies | **N/A** — auth is a stateless Bearer JWT stored client-side (`localStorage`), no server-issued session cookie exists to mark `Secure`. |
| HTTP → HTTPS redirect | Intentionally **not** done in the `frontend` container — it never sees raw internet traffic (Dockploy/your proxy terminates TLS and redirects upstream of it). Documented explicitly so it isn't mistaken for an oversight. |
| HSTS | **Added this phase:** `nginx.conf` emits `Strict-Transport-Security` only when the upstream proxy signals `X-Forwarded-Proto: https` (via an nginx `map`), so it's inert (and harmless) when tested locally over plain HTTP. |
| Additional headers | **Added this phase:** `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, `server_tokens off`. |
| Certificates | **Not generated** — out of scope per instructions; Dockploy/Let's Encrypt or your own proxy handles issuance. |

## 9a. Security Hardening — Fixed Automatically

| Finding | Fix |
|---|---|
| Frontend nginx master process ran as root | Switched to `nginxinc/nginx-unprivileged` — fully non-root. |
| No explicit resource limits (a runaway container could starve the VPS) | Added `deploy.resources.limits` per service. |
| Unbounded container logs (`json-file` default has no rotation) | Added `max-size: 10m`, `max-file: 3` on all services. |
| Implicit Compose network (all services on the same default network, `backend`/`db` reachable by name from anywhere on it) | Explicit `internal` network; still needed for inter-service DNS, but now a deliberate, named, documented boundary. |
| Missing HTTP security headers | Added (see §9). |
| nginx version disclosure | `server_tokens off`. |

## 9b. Security — Manual Review Required

| Item | Why it's not auto-fixed |
|---|---|
| AI provider API key stored in plaintext in the DB (masked in API responses only) | Encryption-at-rest is a schema/code change beyond "prepare deployment config" scope — carried over from Phase 2. |
| Root MySQL user used by the app (`DB_USERNAME: root`) | Works, but a dedicated least-privilege DB user is best practice; changing it means updating the app's datasource config and is a deliberate decision, not a safe automatic edit. |
| No secrets manager (Vault/Doppler/1Password) — `.env` file on the VPS is the secret store | Reasonable for a single-VPS Dockploy deployment; flag for later if the deployment grows multi-host. |
| `Project-Synopsis.pdf` (19 MB) still at repo root (Phase 1 finding, unresolved) | Bloats every `git clone`/CI checkout; move to Git LFS or external storage. |

## 10. Resource Optimisation — VPS Sizing

Estimates based on the actual stack (Spring Boot + JRE, MySQL 8, static nginx) and the
`deploy.resources.limits` set in `docker-compose.yml`.

| Tier | vCPU | RAM | Storage | Network | Notes |
|---|---|---|---|---|---|
| **Minimum** | 2 | 2 GB | 20 GB SSD | 1 TB/mo | Tight — MySQL + JVM heap leave little headroom for the host OS. Fine for a demo/single-school pilot. |
| **Recommended** | 2–4 | 4 GB | 40 GB SSD | 2 TB/mo | Comfortable headroom; matches the Compose limits below with room for OS + Docker overhead. |
| **High-load** (many concurrent assessments/reports) | 4–8 | 8 GB | 80+ GB SSD (consider separate volume for `reports_data`) | 4+ TB/mo | PDF generation (`openhtmltopdf`) is CPU/memory-bursty per report; scale here first if report generation is heavy. |

**Per-container limits configured in Compose (sum ≈ recommended tier):**

| Service | CPU limit | Memory limit |
|---|---|---|
| `db` (MySQL 8) | 1.0 | 768 MB |
| `backend` (Spring Boot, `MaxRAMPercentage=75%`) | 1.5 | 1024 MB (→ ~768 MB JVM heap) |
| `frontend` (static nginx) | 0.5 | 128 MB |
| **Total** | **3.0** | **~1.9 GB** |

A 4 GB VPS leaves ~2 GB headroom for the host OS, Docker daemon, and burst usage (e.g. Flyway
migrations or PDF generation spikes) above the steady-state limits.

## 11. Deployment Validation

Performed without deploying externally, per instructions.

| Check | Result |
|---|---|
| `docker compose config` (env resolution, schema, healthchecks, resource limits) | ✅ Renders correctly — verified with a throwaway local `.env` (deleted after; never committed). |
| Backend Maven build (`mvnw clean package`) | ✅ Verified in Phase 2 — carried forward, no backend logic changed this phase. |
| Frontend build (`npm run build`) | ✅ Verified in Phase 2 — carried forward, no frontend logic changed this phase. |
| Live `docker compose up --build` (actual container build + boot) | **See note below.** |
| `GET /health` from inside a running container | ✅ Verified in Phase 2 (`{"status":"ok","version":"1.0.0"}`) against the packaged jar directly; not yet re-run inside the new Docker image build in this phase. |
| Redis connection | N/A — no Redis in this stack. |
| Missing dependencies / broken imports | None found (static audit; no source changed except config files). |

> **Note on live Docker build:** Docker Desktop was not running in this environment at the start of
> this phase. It was launched, but its backend crashed on startup with:
> `initializing Inference manager: listening on unix://…/dockerInference: remove …/dockerInference:
> The file cannot be accessed by the system` (from `com.docker.backend.exe.log`) — a stale socket file
> left over from a previous Docker Desktop session, unrelated to this project. Removing files under
> the user's `AppData\Local\Docker\run\` directory is outside this project's repo and outside the scope
> of "prepare the project," so it was **not** deleted; this is flagged for the user to clear (delete
> that `run\` folder's contents, or run Docker Desktop's "Reset to factory defaults") and relaunch
> Docker Desktop themselves.
>
> As a result, `docker compose config` (client-side validation, no daemon needed) succeeded, but a full
> `docker compose up -d --build` image build/boot cycle against the live engine could not be completed
> in this environment. **This is reported honestly rather than fabricated.** Everything that does not
> require a running daemon — Dockerfile correctness, Compose schema/env resolution/resource limits,
> `.dockerignore` contents, nginx config syntax by inspection — was reviewed and is believed correct,
> but the actual `docker build` / container boot / health-check-passes end-to-end run is the one
> remaining manual verification step before first production deploy (§12, item 1).

## 12. Deployment Blockers

**None functional.** One verification gap (above) is the only outstanding item:

1. Run `docker compose up -d --build` once on a machine with a running Docker daemon (the target VPS
   itself is the natural place) and confirm all three services reach `healthy` and `GET /health`
   responds — before relying on this for a real deploy.

## 13. Manual Tasks

1. Complete the live `docker compose up -d --build` dry run (§12).
2. `cp .env.example .env`; set strong `DB_PASSWORD`, `JWT_SECRET` (`openssl rand -base64 48`), `TZ`.
3. If deploying outside Dockploy: put a TLS-terminating proxy in front of `${FRONTEND_PORT}`.
4. Schedule the backup commands (§10 of the deployment guide) via cron/Dockploy scheduled jobs.
5. Consider a dedicated least-privilege MySQL user instead of `root` (see §9b).
6. Move `Project-Synopsis.pdf` out of the repo (Phase 1/2 carry-over).

## 14. Monitoring Recommendations

| Concern | Recommendation |
|---|---|
| Uptime / `/health` polling | **UptimeRobot** or **Better Stack** (free tiers suffice) — hit `GET /health` every 1–5 min, alert on non-200 or version-field absence. |
| Container logs | `docker compose logs -f` for ad hoc; for anything durable, ship the `json-file` logs (already rotation-capped at 30MB/service) to **Better Stack Logs** or a self-hosted **Grafana Loki**. |
| Container restarts / crashloops | Dockploy's own dashboard shows restart counts; alternatively `docker events --filter event=restart`. |
| CPU / memory / disk | **Grafana + Prometheus** (via `cadvisor` or `node-exporter`) if you want dashboards; for a single small VPS, Dockploy's built-in resource graphs plus Hostinger's panel may already be enough — don't add Prometheus/Grafana purely for a one-VPS deployment unless you already run them elsewhere. |
| Application errors | The new `GlobalExceptionHandler` logs every unexpected exception at ERROR with a stack trace — ship backend container logs to Better Stack/Loki and alert on `ERROR` frequency spikes. |
| Disk usage (MySQL volume growth, report PDFs) | A simple `df -h` cron + alert threshold, or Hostinger/Dockploy's built-in disk alerts — no extra tool needed at this scale. |

None of the above are implemented in this phase (per instructions: recommend, don't require
implementation) — Dockploy's own dashboard plus UptimeRobot on `/health` is the minimum viable setup
for launch; add Grafana/Prometheus only if/when the deployment outgrows a single VPS.

## 15. Deployment Readiness Score: **90 / 100**

| Category | Score | Notes |
|---|---|---|
| Dockerfile quality (multi-stage, non-root, small image, healthcheck, layer caching) | 19/20 | Fully non-root now (both containers); JVM sized to cgroup memory. |
| Compose correctness (networks, volumes, healthchecks, resource limits, secrets) | 19/20 | Validated with `docker compose config`; live boot not yet run (see §11). |
| Security hardening (headers, non-root, network isolation, no hardcoded secrets) | 18/20 | AI key plaintext at rest and root DB user remain manual items. |
| HTTPS/reverse-proxy readiness | 14/15 | Correct trust-proxy config and headers; certs/redirect correctly deferred to Dockploy. |
| Documentation (deployment guide, update/rollback/backup) | 10/10 | Full guide with runnable commands. |
| Live validation completeness | 10/15 | Static/config validation done; live `docker compose up --build` still pending (§11/§12). |

## 16. Overall Production Risk: **Low–Medium**

Low risk for the application itself (fail-fast config, health-gated startup, non-root everywhere,
resource limits, no secrets in source). Medium only because the live container-boot smoke test
(§11/§12) has not yet been executed end-to-end in this environment — do that once, on real Docker,
before the first production deploy, and risk drops to Low.

## 17. Recommended Next Phase

**Phase 4 — CI/CD & first production deploy**, specifically:
1. GitHub Actions workflow: `docker compose build` on every PR (catches Dockerfile/compose regressions
   automatically instead of relying on manual runs).
2. The one pending manual task from this phase: a real `docker compose up -d --build` dry run.
3. First actual deploy to the Hostinger VPS via Dockploy, followed by the UptimeRobot health check
   (§14) going live.
4. Address the two manual-review security items (§9b) if compliance requirements demand it before
   handling real student data.

---

## Final Validation

- ✅ **Dockerfile is production-ready** — multi-stage, non-root, healthchecks, small runtime images.
- ✅ **Docker Compose is production-ready** — validated via `docker compose config`; networks, volumes,
  resource limits, and fail-fast secret guards all correct.
- ✅ **Dockploy-compatible** — single published port, no redundant reverse proxy, healthchecks Dockploy
  can key off.
- ✅ **Hostinger VPS-compatible** — sized for a 4 GB/2–4 vCPU VPS at the recommended tier (§10).
- ✅ **No hardcoded secrets** — confirmed repo-wide, all secrets are env-var-backed with fail-fast guards.
- ✅ **Persistent data configured correctly** — named volumes for MySQL data and generated reports.
- ✅ **Production configuration validated** — `docker compose config` succeeded; prod Spring profile
  fail-fast validated in Phase 2.
- ✅ **Ready for GitHub** — nothing sensitive staged; `.gitignore`/`.dockerignore` correct.
- ⚠️ **Ready for first production deployment** — yes, pending the one open item in §12 (a live
  `docker compose up --build` dry run on real Docker before going live — the Docker Desktop instance
  in this environment has a stale-socket crash unrelated to the project; see §11 for the exact error
  and fix).

Nothing was deployed. No external push performed.
