# Phase 2 — Production Environment & Release Readiness

**Target release:** v1.0.0
**Date:** 2026-07-29
**Deployment targets:** GitHub · Docker · Dockploy · Hostinger VPS
**Stack:** Spring Boot 2.7 (Java 17) + React 18 (CRA) + MySQL 8. *Not Node/Next.js* —
Node-specific items in the brief (`NODE_ENV`, `.next/`, promise rejections) are mapped to their
Spring/CRA equivalents or marked N/A.

---

## 1. Environment Variables Discovered

Enumerated from `application*.properties` (`${VAR}` placeholders) and `docker-compose.yml`. Frontend
uses **no** `REACT_APP_*` vars (dev proxy target hardcoded in `setupProxy.js`).

| Variable | Required | Where |
|---|---|---|
| `DB_URL` | no (default) | backend datasource |
| `DB_USERNAME` | no (default `root`) | backend datasource |
| `DB_PASSWORD` | **yes** (prod: no default → fail fast) | backend datasource, MySQL container |
| `DB_NAME` | no (default `exam-portal`) | Compose |
| `JWT_SECRET` | **yes** (prod: validated) | JWT signing |
| `SUPERADMIN_USERNAME/PASSWORD/FIRST_NAME/LAST_NAME/PHONE` | no | first super-admin seed |
| `MENTALIST_REPORTS_DIR` | no (default `./data/mentalist-reports`) | PDF output |
| `SPRING_PROFILES_ACTIVE` | prod | profile selection |
| `FRONTEND_PORT` | no (default `8080`) | Compose host port |
| `app.version` | build-filtered | surfaced by `/health` |

Templates written: **`.env.example`** (repo root, Compose) and **`exam-portal-backend/.env.example`**
(backend, non-Docker). Both grouped by category with comments and safe placeholders.

## 2. Missing Variables

None missing for the app to run. The **AI provider API key is intentionally not an env var** — it is
configured at runtime by a super admin and stored (masked) in the DB. No email/object-storage vars
because the app has neither integration.

## 3. Secrets Audit

✅ **No secrets in source.** Repo-wide scan (`API key`, `password`, `token`, `secret`, `BEGIN … PRIVATE
KEY`, `.pem`/`.p12`/`.key` files): nothing hardcoded.

- All backend secrets use `${VAR}` env interpolation.
- The only in-repo "secret" is the **development JWT fallback** in `application.properties`
  (`exam-portal-dev-secret-change-me`) — clearly labelled dev-only and **rejected at startup under the
  `prod` profile** (verified).
- No `.env`, keystore, or certificate is tracked.
- AI key: DB-stored, masked in `AiSettingsDto`.

## 4. Repository Audit

- `.gitignore` now covers `node_modules/`, `target/`, `build/`, `dist/`, `.next/`, `coverage/`,
  `.cache/`, `uploads/`, `tmp/`, `*.log`, `.env`/`.env.local`/`.env.production`/`.env.*.local`,
  `.idea/`, `.vscode/`, `*.iml`, `.DS_Store`, `Thumbs.db`.
- `.dockerignore` added for both images (excludes `target/`, `node_modules/`, `.git`, `.env`, logs).
- Build artifacts and secrets cannot be committed accidentally.

## 5. Configuration Improvements

| Area | Change |
|---|---|
| Prod profile | New `application-prod.properties`: secrets with **no defaults** (fail fast), `show_sql=false`, no stacktrace/exception in errors, INFO/WARN logging. |
| Fail-fast validation | New `EnvironmentValidator` (`@Profile("prod")`): rejects missing / dev-default / <32-char `JWT_SECRET` with a clear `FATAL: production configuration error — …` message. **Verified** (5-char secret aborts startup). |
| Health check | New `GET /health` → `{"status":"ok","version":"1.0.0"}`, public, no DB/auth. **Verified 200.** |
| Error handling | New `GlobalExceptionHandler` (extends `ResponseEntityExceptionHandler`): preserves `ResponseStatusException` contract (verified bad-login still returns `{"message":…}` 401), logs unexpected errors at ERROR, returns opaque 500. Standard MVC 4xx untouched. |
| Graceful shutdown | `server.shutdown=graceful` for clean container SIGTERM. |
| Versioning | pom `0.0.1-SNAPSHOT` → `1.0.0`; `VERSION` file; `app.version` build-filtered into `/health`. |
| Docker | Backend multi-stage (Maven→JRE, non-root, HEALTHCHECK); frontend (build→nginx, `/api` proxy, SPA fallback); `docker-compose.yml` (MySQL+backend+frontend, health-gated `depends_on`, named volumes). |

Dev-only features that must not run in prod: **none active** — CRA `setupProxy.js` runs only under
`react-scripts start`, never in the production build; `spring-boot-devtools` is `optional`/`runtime`
and inert in the packaged jar. No mock APIs, dev banners, or fake services exist.

## 6. Build Verification

| Build | Result |
|---|---|
| Backend `mvnw clean package -DskipTests` | ✅ Exit 0 → `exam-portal-backend-1.0.0.jar` (50 MB fat jar) |
| Frontend `npm run build` | ✅ Exit 0 → `build/` ready (warnings only, no errors) |
| Prod jar boot (valid env) | ✅ `Started … in 32s`, profile `prod` active |
| `GET /health` | ✅ `200 {"status":"ok","version":"1.0.0"}` |
| Protected route unauthenticated | ✅ `403` (auth intact) |
| Fail-fast (short secret) | ✅ Aborts with clear FATAL message |
| Broken imports / startup errors | None |

TypeScript errors: N/A (JS project). Missing-env at build: none.

## 7. Production Blockers

**None blocking a v1.0.0 containerised deploy.** The stack builds, boots under `prod`, validates its
config, and serves health. Items below are hardening, not blockers.

## 8. Manual Tasks (before/at deploy)

1. `cp .env.example .env`; set a strong `DB_PASSWORD` and a ≥32-char random `JWT_SECRET`
   (`openssl rand -base64 48`); optionally `SUPERADMIN_PASSWORD` to seed the first admin.
2. Put a **TLS-terminating reverse proxy** (Dockploy/Traefik/nginx) in front of the `frontend`
   service — bundled nginx serves plain HTTP inside the network.
3. Ensure volumes `db_data` and `reports_data` are persisted/backed up (see README Backup).
4. Do **not** expose MySQL publicly (Compose keeps it internal — keep it that way).
5. Consider self-hosting Bootstrap (currently a **CDN beta** in `index.html`) for CSP/offline
   (carried over from Phase 1).

## 9. Deployment Readiness Score: **88 / 100**

| Category | Score |
|---|---|
| Build reproducibility (Docker, multi-stage, pinned base images) | 19/20 |
| Secret management (env-based, fail-fast, no leaks) | 18/20 |
| Config & profiles (prod hardening, validation) | 18/20 |
| Observability (health check, structured logging) | 13/15 |
| Error handling & graceful shutdown | 13/15 |
| Docs (README, CHANGELOG, .env templates) | 7/10 |

Deductions: no TLS inside the stack (external proxy assumed), AI key plaintext at rest, Bootstrap CDN
beta, no automated CI pipeline, `/health` is liveness-only (no DB readiness probe).

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Weak/absent `JWT_SECRET` in prod | Low | High | **Mitigated** — startup fails fast with a clear message. |
| Secret leakage via git | Low | High | `.gitignore` + `.dockerignore` cover `.env`; no secrets in source. |
| DB data loss | Medium | High | Named volume + documented `mysqldump` backup; automate off-box. |
| AI key exposure at rest | Low | Medium | DB-stored, masked in API; encrypt-at-rest recommended. |
| No TLS if proxy misconfigured | Medium | High | Document + require external TLS proxy before exposure. |
| Supply chain (Bootstrap CDN beta) | Low | Medium | Self-host stable 5.3.x. |

## 11. Recommended Next Steps

1. Add a CI workflow (GitHub Actions): build both images, run backend tests, publish on tag.
2. Encrypt the AI key at rest (or move to a secrets manager).
3. Self-host a stable Bootstrap; drop the CDN beta and the unused `bootstrap` npm dep.
4. Add a `/health/ready` readiness probe that checks the DB (keep `/health` as liveness).
5. Replace the stale CRA `App.test.js` with a real smoke test; wire it into CI.
6. Externalize CORS origins for prod.

---

## Final Validation

- ✅ **Ready to commit to GitHub** — no secrets, `.gitignore` complete, builds pass.
- ✅ **Ready for Docker build** — both Dockerfiles + `.dockerignore` present; jar and bundle build clean.
- ✅ **Ready for Dockploy** — `docker-compose.yml` with health-gated services and named volumes.
- ✅ **Ready for Hostinger VPS** — Compose-based, MySQL kept internal, external TLS documented.

Nothing was deployed. All artifacts and documentation prepared for a v1.0.0 release.
