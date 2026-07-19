# Exam Portal — Project Guide

Full-stack quiz platform. Spring Boot REST API + React SPA + MySQL. Three roles:
`SUPER_ADMIN` (platform owner) > `ADMIN` (teacher/school) > `USER` (student).
Admins own categories/quizzes/questions; students take quizzes and see scorecards.

## Layout

```
exam-portal-backend/    Spring Boot 2.7.3, Java 17, port 8081
exam-portal-frontend/   React 18 (CRA), Redux-thunk, Bootstrap
```

Backend base package: `com.project.examportalbackend`, organized by layer
(`controllers` → `services`/`services.implementation` → `repository`,
plus `configurations`, `security`, `dto`, `models`, `bootstrap`).

## Commands

**Backend** (needs MySQL on `:3306`, JDK **17** — not 25):
```bash
cd exam-portal-backend
./mvnw spring-boot:run          # dev run, applies Flyway V1–V7 on startup
./mvnw clean package            # build jar → target/
./mvnw test
```
Env vars (defaults in `application.properties`):
- `JWT_SECRET` — **must set in any real run**; falls back to a public dev secret silently
- `SUPERADMIN_PASSWORD` — first SUPER_ADMIN seeded on boot only if set (else skipped with a warning)
- `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` — default `root`/`root@`, DB auto-created

**Frontend**:
```bash
cd exam-portal-frontend
npm install --legacy-peer-deps  # required — peer-dep conflicts otherwise
npm start                        # :3000, proxies /api → 127.0.0.1:8081 via setupProxy.js
npm run build                    # static bundle → build/
```

## Architecture rules (follow the existing pattern, not generic advice)

### Backend — every write goes through an ownership check
- Controllers are **thin**: `ResponseEntity<?>`, delegate straight to the service. See
  [CategoryController.java](exam-portal-backend/src/main/java/com/project/examportalbackend/controllers/CategoryController.java).
- Business logic + validation live in `*ServiceImpl`. Validate with `StringUtils.hasText(...)`
  and throw `ResponseStatusException(HttpStatus.X, "message")` — this is the **only** error
  mechanism (no `@ControllerAdvice` exists).
- **Multi-tenancy is enforced in the service, not the controller.** Use
  [`AuthFacade`](exam-portal-backend/src/main/java/com/project/examportalbackend/security/AuthFacade.java):
  - stamp `createdBy` from `authFacade.getCurrentUserId()` on create — **never** from the request body
  - call `authFacade.assertCanManage(existing.getCreatedBy())` before any update/delete
  - scope reads: ADMIN → `findByCreatedBy(me)`; student → `findByCreatedBy(teacherId)`; SUPER_ADMIN → `findAll()`
  - `CategoryServiceImpl` is the reference implementation of all four.
- Add a matching URL rule in
  [`SecurityConfig`](exam-portal-backend/src/main/java/com/project/examportalbackend/configurations/SecurityConfig.java).
  Default is `anyRequest().denyAll()` — an endpoint with no rule is blocked.
- Repositories: derived query methods only (`findByCreatedBy`, `existsByTitleIgnoreCaseAndCreatedBy`).
  No `@Query`/JPQL in this codebase.

### Backend — DO NOT leak entities (known active bugs — don't copy or spread them)
- **Never return a raw `User`** — it serializes the BCrypt `password` (no `@JsonIgnore`).
  Map to a DTO. `AuthController.register*` and `LoginResponse` currently leak it; don't add more.
- **Never return a raw `Question` to a non-admin** — `answer` is not `@JsonIgnore`d. Use
  [`ExamQuestionDto`](exam-portal-backend/src/main/java/com/project/examportalbackend/dto/ExamQuestionDto.java)
  (the answers-stripped shape) as the pattern.
- DTOs use a static `from(entity)` factory. Follow `AdminDto`/`StudentDto`/`ReportRowDto`.

### Frontend — Redux-thunk slice per resource
Each backend resource has a matching set of 4 files, all wired by domain name:
`constants/<x>Constants.js` (REQUEST/SUCCESS/FAILURE strings) → `services/<x>Services.js`
(raw axios) → `actions/<x>Actions.js` (`(dispatch, ...args, token)` thunk) → `reducers/<x>Reducer.js`
→ registered in `store.js`. `categories` is the cleanest reference set.

- JWT lives in `localStorage.jwtToken` (raw string), user in `localStorage.user` (JSON).
  Callers read the token themselves: `JSON.parse(localStorage.getItem("jwtToken"))` and pass it into the thunk.
- There is **no shared axios instance** — each service builds
  `{ headers: { Authorization: \`Bearer ${token}\` } }` by hand and calls a relative `/api/...` path.
- Route guarding is `components/ProtectedRoute.js` (`adminRoute`/`userRoute`/`superAdminRoute` in `App.js`).
  It is **UI convenience only** — real enforcement is server-side. Never rely on it for security.
- Components are functional + hooks. No TypeScript, no PropTypes, no Context API.

## Database
Schema is owned by **Flyway** (`resources/db/migration/V1..V7`), Hibernate is `ddl-auto=validate`.
Never change schema via entity annotations alone — add a migration (see the `flyway-migration` skill).
Entity↔column quirks to know: `Quiz.iActive` (`is_active`), `QuizResult.attemptDatetime` is a **String**,
`QuizResult.userId` / `Category.createdBy` / `Quiz.createdBy` are raw `Long` FKs (not `@ManyToOne`).

## Conventions
- Backend: Lombok `@Getter/@Setter/@ToString` (not `@Data`). PascalCase classes, layer packages.
- Frontend: PascalCase components/pages, camelCase services/actions/reducers/constants.
- Commit style: Conventional Commits (`feat:`/`fix:`/…).

## Testing log
`.claude/testing-log.md` is an append-only record of live-testing events, runtime facts (env vars,
DB state), and findings — **read it before starting work, append an entry after every change/test.**
The `backend-endpoint` and `frontend-resource` skills require logging on every update.

## Skills
- `run-local` — bring up MySQL + backend + frontend and smoke-test
- `backend-endpoint` — add a REST endpoint end-to-end with ownership scoping
- `flyway-migration` — add a schema migration correctly
- `frontend-resource` — wire a new Redux-thunk resource slice
- `security-review` — check a change against this repo's known leak/authz pitfalls
