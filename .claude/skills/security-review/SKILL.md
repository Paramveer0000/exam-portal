---
name: security-review
description: Review an Exam Portal change against this repo's known, specific security pitfalls — entity leakage, missing ownership checks, CORS, secrets, error disclosure. Use before committing backend changes that touch controllers, services, security config, or entities.
---

# security-review

Check the diff against the concrete, already-present issues in this codebase. These are real —
some are active bugs. Priority order: leaks and authz first.

## 1. Entity leakage (highest)
- **Raw `User` returned anywhere?** `User.password` (BCrypt hash) has no `@JsonIgnore`, so any
  endpoint returning a `User` serializes the hash. Known offenders: `AuthController.registerUser`,
  `registerSchool`, and everything wrapping `LoginResponse`. Do not add more — map to a DTO
  (`AdminDto`/`StudentDto`/`TeacherDto` with `from(entity)`).
- **Raw `Question` returned to a non-admin?** `Question.answer` isn't `@JsonIgnore`d. Only the
  `/api/question/**` → ADMIN URL rule protects it. Student-facing quiz data must go through
  `ExamQuestionDto` (answers stripped). Any new endpoint that returns `Question` to `USER` leaks answers.

## 2. Ownership / authz
- Every update/delete calls `authFacade.assertCanManage(existing.getCreatedBy())` **before** mutating.
- `createdBy`/owner ids are stamped from `authFacade.getCurrentUserId()`, never read from the request body.
- Reads are role-scoped (ADMIN→own, student→teacher's, SUPER_ADMIN→all).
- New endpoint has a matching rule in `SecurityConfig`? Default is `denyAll()`; a missing rule
  blocks the endpoint (fail-safe), but a **too-broad** rule is the risk — GET open to USER, mutations ADMIN-only.

## 3. Config-level (flag if the change reintroduces or worsens)
- **JWT secret**: `jwt.secret=${JWT_SECRET:exam-portal-dev-secret-change-me}` — silent public fallback.
  Any real run must set `JWT_SECRET`. Don't hardcode a new secret.
- **CORS**: bare `@CrossOrigin` on every controller + `http.cors()` with no `CorsConfigurationSource`
  bean = allow-all origins. If tightening, add the bean; if adding a controller, know it inherits this.
- **Error disclosure**: `server.error.include-message=always` + no `@ControllerAdvice`. Raw exception
  messages reach clients. Don't put SQL/internal detail into a `ResponseStatusException` reason string.
- **NPE → 500**: guard request-body relations (e.g. `question.getQuiz()`) instead of dereferencing —
  an unhandled NPE returns a 500 with a leaked message rather than a clean 400.

## 4. Frontend
- `ProtectedRoute` is UI-only; never treat it as an authorization boundary — the server must enforce.
- Token in `localStorage.jwtToken` (XSS-reachable) — don't widen exposure (e.g. logging tokens,
  putting them in URLs/query strings).

## Output
List findings as `file:line — issue — fix`, most severe first. Confirm the ownership check and the
DTO mapping are present for any new write endpoint. Don't flag the whole-repo stale-dependency /
architecture items here (Boot 2.7.3 EOL, jjwt 0.9.1, no Bean Validation) unless the diff touches them —
those live in the architecture report, not per-change review.
