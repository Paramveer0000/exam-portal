# Phase 4 — v1.0.0 Release Checklist

**Date:** 2026-07-29
**Builds on:** Phase 1 (cleanup), Phase 2 (env/release prep), Phase 3 (Docker hardening).
This phase re-verifies everything end to end and produces the go/no-go call for tagging `v1.0.0`.

---

## 1. Build Verification

| Check | Result |
|---|---|
| Backend `mvnw clean package -DskipTests` | ✅ Exit 0 → `exam-portal-backend-1.0.0.jar` (~50 MB) |
| Frontend `npm run build` | ✅ Exit 0 → `build/` (144 KB gzipped JS, 4.4 KB CSS) |

Both rebuilt fresh for this phase, not carried over by assumption.

## 2. Working Tree / Temporary Files

| Check | Result |
|---|---|
| `git status` | ✅ Clean before this phase's edits |
| Stray `*.tmp`/`*.bak`/`*.copy` files | ✅ None found |
| Temporary validation files created this phase (`.env.phase4test`) | ✅ Created, used, deleted — never committed |

## 3. `.gitignore`

✅ Correct and complete (confirmed by re-read): `node_modules/`, `target/`, `build/`, `dist/`,
`.next/`, `coverage/`, `.cache/`, backend `data/`, `uploads/`, `tmp/`, `*.log` + npm/yarn debug logs,
`.env`/`.env.local`/`.env.production`/`.env.*.local`, `.idea/`, `.vscode/`, `*.iml`, OS junk.

## 4. `.env` / `.env.example`

| Check | Result |
|---|---|
| `.env` tracked in git | ✅ No (`git ls-files` confirms) |
| Root `.env.example` complete | ✅ All Compose-consumed vars present, grouped, commented (DB, JWT, super-admin seed, web/networking incl. `TZ`, explicit notes on AI-key/email/S3 being N/A) |
| Backend `.env.example` complete | ✅ All 10 backend vars documented |

## 5. README.md

Updated this phase (stale details found and fixed):
- Backup command referenced the wrong Compose volume name (`examportalmaster_reports_data` →
  corrected to `exam-portal_reports_data`, matching Phase 3's explicit `name: exam-portal`).
- Folder structure was missing `docs/deployment/` (added in Phase 3).
- `TZ` environment variable was undocumented in the variable table (added).
- Added a direct link to `docs/deployment/docker-deployment.md` from the Production Deployment section.

All requested sections already present from Phase 2 and confirmed still accurate: overview,
requirements, installation, environment variables, Docker setup, running locally, production
deployment, database migration, backup, update process, troubleshooting, folder structure.

## 6. CHANGELOG.md

Updated this phase — the `v1.0.0` entry predated Phase 3's Docker hardening work, so it was missing:
- Non-root frontend container (`nginx-unprivileged`), security headers, HSTS.
- Named containers, isolated internal network, per-service resource limits, rotated logging.
- The new deployment guide link.

`Known Limitations` section re-checked against current state — still accurate (see §10 below for
what's still open).

## 7. VERSION

✅ `VERSION` file contains `1.0.0`. ✅ `exam-portal-backend/pom.xml` `<version>1.0.0</version>` —
confirmed matching (both re-read this phase, not assumed from memory).

## 8. Dockerfile / docker-compose.yml Production Readiness

Both already hardened in Phase 3; re-verified this phase:

| Check | Result |
|---|---|
| `docker compose config` (client-side validation, throwaway `.env` deleted after) | ✅ Valid — re-run this phase, second confirmation beyond Phase 3's |
| Non-root execution (backend + frontend) | ✅ Both containers run as non-root (`app` user; `nginx-unprivileged`) |
| Health checks (all 3 services) | ✅ Present |
| Resource limits (all 3 services) | ✅ Present |
| Secrets have no real defaults in compose (`DB_PASSWORD`, `JWT_SECRET` use `${VAR:?...}`) | ✅ Confirmed |
| Named volumes for persistent data | ✅ `db_data`, `reports_data` |
| Live `docker compose up --build` end-to-end boot | ⚠️ **Not possible in this environment** — see §10, blocker #1. Not fabricated. |

## 9. Release Checklist

- [x] Backend builds (`mvnw clean package`) — exit 0
- [x] Frontend builds (`npm run build`) — exit 0
- [x] Working tree clean, no stray temp files
- [x] `.gitignore` covers env/build/IDE/OS artifacts
- [x] `.env` not tracked; both `.env.example` files complete
- [x] README accurate and complete (stale volume-name/folder-structure fixed this phase)
- [x] CHANGELOG `v1.0.0` entry reflects the actual shipped state (Phase 3 additions folded in)
- [x] `VERSION` = `1.0.0`, matches `pom.xml`
- [x] Dockerfiles: multi-stage, non-root, healthchecked
- [x] `docker-compose.yml`: named, networked, resource-limited, no hardcoded secrets
- [ ] **One live `docker compose up -d --build` dry run on a working Docker daemon** — see blocker below
- [ ] Tag `v1.0.0` (not performed — deploy/tagging explicitly out of scope for this phase)

## 10. Blockers Before Deployment

**1. Live Docker build/boot not verified end-to-end (carried over from Phase 3, still open).**
Docker Desktop in this environment fails to start its backend:
```
initializing Inference manager: listening on unix://…/dockerInference: remove …/dockerInference:
The file cannot be accessed by the system
```
A stale socket file under the user's `AppData\Local\Docker\run\`, left from a prior Docker Desktop
session — unrelated to this project. Fixing it means deleting files outside the repo (system AppData),
which is out of scope for "prepare the repository" and was correctly left untouched.
**Action needed from you:** clear that Docker Desktop state (delete the `run\` folder's contents, or
Docker Desktop → Troubleshoot → "Reset to factory defaults"), then run
`docker compose up -d --build` once and confirm all three services report `healthy` before the first
real deploy. Everything short of the live daemon (Dockerfile correctness, Compose schema/env
resolution, resource limits) has been validated twice now (Phase 3 and this phase) via
`docker compose config`.

**No other blockers.** Both application builds pass, no secrets in source, all docs current.

## 11. Non-Blocking Known Debt (carried over, not re-litigated this phase)

- `Project-Synopsis.pdf` (19 MB) still committed at repo root — bloats every clone; move to
  Git LFS/external storage when convenient.
- `App.test.js` is still the unmodified CRA sample (`renders learn react link`) — would fail if
  actually run; replace with a real smoke test.
- AI provider API key stored in plaintext in the DB (masked in API responses only).
- App runs as `root` MySQL user; a dedicated least-privilege DB user is recommended but not required
  to ship v1.0.0.

None of these block tagging or deploying `v1.0.0` — they're flagged for a follow-up pass.

---

## Final Output

- ✅ **Repository ready for GitHub** — clean tree, no secrets, `.gitignore` correct.
- ✅ **Ready to create tag `v1.0.0`** — `VERSION` and `pom.xml` match, both builds pass, docs current.
- ⚠️ **Ready for Dockploy deployment** — yes, pending the one open item in §10 (a live
  `docker compose up --build` dry run on a working Docker daemon; the config itself validates clean).
- ✅ **No deployment performed** — nothing pushed, nothing tagged, nothing deployed.
