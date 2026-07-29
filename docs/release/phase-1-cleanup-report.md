# Phase 1 — Production Cleanup & Codebase Audit

**Target release:** v1.0.0
**Date:** 2026-07-29
**Scope:** Safe cleanup only. No business logic, auth, schema, or API behavior changed.
**Stack:** Spring Boot 2.7 (Java 17) backend · React 18 (CRA) + Bootstrap frontend · MySQL/Flyway.

> Note on stack: several audit sub-tasks assume Next.js/TypeScript/Tailwind. This project is
> **JavaScript CRA + Bootstrap**, so "TypeScript errors", "hydration errors", and "SSR" do not
> apply. Those items are marked N/A below rather than silently skipped.

---

## 1. Files Deleted

| File(s) | Count | Why | Verification |
|---|---|---|---|
| `images/*.png` (root) | 20 | Orphaned screenshots. README embeds **GitHub-hosted** image URLs (`user-images.githubusercontent.com`), not these. No code references. | `grep` of `README.md`, `src/`, `public/` — zero refs. `public/images/user.png` (the one asset the app *does* use, via `ProfilePanel.js`) is untouched. |
| `exam-portal-frontend/.idea/*` | 5 | JetBrains editor settings — should never be tracked. | Untracked via `git rm --cached` (files kept on disk, now git-ignored). |

**Total removed from version control: 25 files.**

## 2. Files Modified

| File | Change | Type |
|---|---|---|
| `configurations/JwtRequestFilter.java` | 4× `System.out.println` → SLF4J `log.debug(...)`. Malformed/expired tokens are normal traffic, so `debug` (not `error`). | Debug cleanup |
| `services/authServices.js` | Removed 2 `console.log` (one leaked `data.user` to console). `console.error` kept. | Debug cleanup |
| `services/categoriesServices.js` | Removed 3 `console.log`; dropped now-unused `const { data }` in `deleteCategory`. | Debug cleanup |
| `services/questionsServices.js` | Removed 5 `console.log`; dropped unused `data` in `deleteQuestion`. | Debug cleanup |
| `services/quizzesServices.js` | Removed 5 `console.log`; dropped unused `data` in `deleteQuiz`. | Debug cleanup |
| `services/quizResultServices.js` | Removed 2 `console.log`. | Debug cleanup |
| `pages/users/UserQuestionsPage.js` | Removed 1 debug `console.log("timeRemaining", …)`. | Debug cleanup |
| `pages/users/UserQuizzesPage.js` | Removed 3 dead `onClick={() => console.log("View")}` on display chips (Minutes/Questions/Marks). Chips were never interactive; behavior unchanged. | Dead code |
| `components/Header.js` | Removed dead `profilePageUrl` var + its dead `.map` in a `useEffect` (assigned, never read; also removed a latent null-deref on `loginReducer.user`). Kept `setIsLoggedIn(true)`. | Dead code |
| `public/index.html` | CRA placeholder `<title>React App</title>` → `The Mentalist`; default description → real one. | SEO |
| `.gitignore` | Added `.env*`, `.idea/`, `.vscode/`, `*.iml`, OS junk, npm/yarn debug logs. | Repo hygiene |

**New files:** `exam-portal-backend/.env.example` (all 10 backend env vars documented with safe placeholders).

Total `console.log`/`console.debug` removed: **23**. `console.error` retained: **22**.
Backend `System.out.println` removed: **4**. `printStackTrace`: **0** (none existed).

## 3. Dependencies

No dependencies removed (conservative — repo already requires `--legacy-peer-deps`, so touching the
dependency tree risks install breakage). Findings only:

| Package | Finding | Recommendation |
|---|---|---|
| `bootstrap ^5.2.0` | **Effectively unused as an npm dep** — Bootstrap CSS is loaded from a **CDN** in `index.html`; JS comes via `react-bootstrap`. Nothing `import`s the `bootstrap` package. | Either self-host (import the local package CSS, drop the CDN) **or** remove the dep. Self-hosting is the better prod/CSP/offline choice. |
| Bootstrap CDN | Pinned to **`5.2.0-beta1`** (a beta) via jsDelivr with an SRI hash. | Ship a **stable** `5.3.x`, preferably self-hosted, not a beta from a third-party CDN. |
| `redux-devtools-extension ^2.13.9` | **Deprecated** package. Still used in `store.js` (`composeWithDevTools`). Works, but unmaintained. | Migrate to `@redux-devtools/extension`. |
| `axios ^0.27.2` | Very old (0.x). Not vulnerable in current use, but pre-1.0. | Consider `axios@^1.x` in a later phase (minor API deltas). |
| `web-vitals`, `@testing-library/*` | CRA scaffolding. `web-vitals` is imported dynamically by `reportWebVitals.js`; `reportWebVitals()` is called with **no callback**, so it is effectively inert but harmless. | Keep (standard CRA). |

No duplicate packages detected. Backend `pom.xml` not pruned (out of safe scope for this phase).

## 4. Environment / Secrets

✅ **No hardcoded secrets, API keys, or passwords in source.**

- All backend config uses `${VAR:default}` env interpolation (`application.properties`).
- AI provider API key is stored in the DB (`platform_ai_settings.api_key`) and **masked** in
  `AiSettingsDto` — never returned raw to clients.
- No `.env` file is tracked; none contained secrets.
- Added **`exam-portal-backend/.env.example`** documenting: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
  `JWT_SECRET`, `SUPERADMIN_USERNAME/PASSWORD/FIRST_NAME/LAST_NAME/PHONE`, `MENTALIST_REPORTS_DIR`.
- Frontend uses **no** `REACT_APP_*` vars (dev proxy target is hardcoded in `setupProxy.js`), so no
  frontend `.env.example` is needed.

⚠️ **Manual review (see §10):** the `JWT_SECRET` dev fallback and the plaintext AI key column.

## 5. Repository Cleanup

`.gitignore` now covers: `node_modules/`, `target/`, `build/`, `data/`, `*.log`, npm/yarn debug
logs, `.env*`, `.idea/`, `.vscode/`, `*.iml`, `.DS_Store`, `Thumbs.db`. Tracked `.idea/` removed
from the index.

## 6. Build Validation

| Check | Result |
|---|---|
| Backend compile (`mvnw compile`, JDK 17) | ✅ Exit 0 |
| Frontend compile (CRA dev server) | ✅ Compiled (warnings only, no errors) |
| Backend still serving | ✅ `GET /api/teachers` = 200 after changes |
| Frontend runtime | ✅ `/` and `/login` render, **zero console errors** |
| Broken imports / circular deps | None found |
| TypeScript errors | N/A (JavaScript project) |
| Hydration errors | N/A (client-only CRA, no SSR) |

**ESLint:** all warnings I *introduced* were fixed (3 unused-`data` after log removal). Header dead-code
warnings resolved. Remaining warnings are **pre-existing** and left as-is (see §Remaining debt).

## 7. Code Quality

Removed dead variables, dead click handlers, and debug logging. Did **not** reformat files wholesale
or reorder imports (would create noisy diffs and risk merge conflicts for marginal gain). No unreachable
code found beyond what's listed in §2.

## 8. Performance

- **Bundle impact: ~0.** The deleted `images/` were repo-root doc assets, never bundled. `console.log`
  removal is negligible. Honest estimate: **frontend bundle unchanged**; the win is repo size and log
  noise, not bundle bytes.
- **Repo size:** ~1.6 MB reclaimed (20 screenshots).
- Largest real bloat is **`Project-Synopsis.pdf` (19 MB)** committed at repo root — see §10.
- `spring.jpa.properties.hibernate.show_sql=true` logs every SQL statement — noise + minor info
  exposure in prod (see §10).

## 9. Security Findings

| Item | Status |
|---|---|
| Hardcoded credentials | ✅ None |
| `eval()` | ✅ None |
| XSS (`dangerouslySetInnerHTML` / `innerHTML`) | ✅ None |
| SQL injection | ✅ None — all repositories use JPA derived queries; no `@Query`/native SQL/`Statement` |
| AuthN / AuthZ checks | ✅ Enforced server-side (`SecurityConfig` default-deny + per-service ownership via `AuthFacade`). UI guards are convenience only. |
| Missing validation | Not a finding in scope — services validate with `StringUtils.hasText` + `ResponseStatusException`. |

## 10. Potential Issues — Manual Review Required

1. **`JWT_SECRET` silent dev fallback** — if the env var is unset, the app boots with a **public,
   in-repo default secret** (`exam-portal-dev-secret-change-me`). For prod, **fail fast** when
   `JWT_SECRET` is absent instead of falling back. *(Behavior change — deferred to Phase 2.)*
2. **`hibernate.show_sql=true`** — set to `false` for production (log noise + query/PII exposure).
3. **AI API key stored plaintext** in `platform_ai_settings.api_key`. Consider encryption at rest.
4. **Bootstrap beta via CDN** — self-host a stable `5.3.x`; a third-party CDN is a supply-chain +
   offline + CSP concern for prod. (Also lets you drop the unused `bootstrap` npm dep.)
5. **`Project-Synopsis.pdf` (19 MB)** and two more root PDFs (~2 MB) are committed. Move to
   releases/Git LFS or an external store; they bloat every clone.
6. **`seed/psychometric/`** (question banks + `seed-banks.ps1` + validator) — **NOT deleted.** This is
   the only source of the psychometric question-bank content and its scoring validator. Keep. It is
   dev/ops tooling (seeds via the live API into a demo school `psychobank`), not app runtime.
7. **`App.test.js`** is the default CRA sample test asserting "learn react" text the app never
   renders — it would **fail** if run. Update it to a real smoke test or remove it. *(Not touched.)*

## Remaining Technical Debt (reported, not changed — behavior-risky)

Pre-existing ESLint warnings, left intentionally (fixing them can change runtime behavior):
- `UserQuestionsPage.js`: `no-extend-native` (mutates `Number.prototype`), 3× `eqeqeq` (`==`),
  4× `react-hooks/exhaustive-deps`.
- `UserQuizzesPage.js`: 2× `exhaustive-deps`, 1× `array-callback-return`.

These belong in a dedicated correctness pass with test coverage, not a "safe cleanup" phase.

---

## Sign-off

- ✅ **Project builds successfully** — backend `compile` exit 0; frontend compiles (warnings only).
- ✅ **No production functionality changed** — verified: backend `GET /api/teachers` = 200, `/` and
  `/login` render with zero console errors, auth flow untouched.
- ✅ **Ready for Phase 2 (Environment & Release Preparation)** — with the §10 items as Phase-2 inputs
  (JWT fail-fast, `show_sql=false`, Bootstrap self-host, PDF/LFS, AI-key encryption).
