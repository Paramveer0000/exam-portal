---
name: backend-endpoint
description: Add or modify a Spring Boot REST endpoint in the Exam Portal backend following this repo's thin-controller + service-ownership pattern. Use when adding a controller method, service operation, or repository query on the backend.
---

# backend-endpoint

Every write in this codebase routes through an ownership check in the **service**, not the
controller. `CategoryServiceImpl` + `CategoryController` are the reference; copy their shape.

## The 5 places a new endpoint touches

1. **Repository** — derived query methods only, no `@Query`/JPQL.
   ```java
   List<Quiz> findByCreatedBy(Long createdBy);
   boolean existsByTitleIgnoreCaseAndCreatedBy(String title, Long createdBy);
   ```

2. **Service interface** (`services/XService.java`) — one method signature.

3. **Service impl** (`services/implementation/XServiceImpl.java`) — the real work:
   ```java
   @Override
   public X addX(X x) {
       if (!StringUtils.hasText(x.getTitle()))
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
       Long me = authFacade.getCurrentUserId();
       // uniqueness / other guards ...
       x.setCreatedBy(me);            // stamp ownership from the token, NEVER the request body
       return xRepository.save(x);
   }

   @Override
   public X updateX(X x) {
       X existing = xRepository.findById(x.getId())
           .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
       authFacade.assertCanManage(existing.getCreatedBy());   // ownership gate before any mutate
       x.setCreatedBy(existing.getCreatedBy());                // preserve owner; body can't reassign
       return xRepository.save(x);
   }
   ```
   Read scoping (see `CategoryServiceImpl.getCategories`):
   - `authFacade.hasRole(AuthFacade.ROLE_ADMIN)` → `findByCreatedBy(getCurrentUserId())`
   - `authFacade.isStudent()` → `findByCreatedBy(getTeacherId())` (null teacherId → empty list)
   - else (SUPER_ADMIN) → `findAll()`

4. **Controller** (`controllers/XController.java`) — thin, `@CrossOrigin`, returns `ResponseEntity<?>`:
   ```java
   @PostMapping("/")
   public ResponseEntity<?> addX(@RequestBody X x) { return ResponseEntity.ok(xService.addX(x)); }
   ```
   No business logic, no auth checks here.

5. **SecurityConfig** — add a URL/method rule in
   `configurations/SecurityConfig.java`. Default is `anyRequest().denyAll()`, so an endpoint
   with **no rule is blocked**. `RoleHierarchy` makes SUPER_ADMIN ⊇ ADMIN, so an
   `hasAuthority("ADMIN")` rule also admits super admins — but don't assume that's always the right
   split. Two different patterns coexist in this codebase, check which one your resource matches:
   - **Content** (category/quiz/question): GET → `hasAnyAuthority("USER","ADMIN")`, every mutation
     (POST/PUT/DELETE) → `hasAuthority("SUPER_ADMIN")` only. A plain ADMIN (school) can read but
     never write content — this changed 2026-07-20; don't copy the older "mutations ADMIN-only"
     pattern for a content-adjacent resource without checking first.
   - **School-owned data** (students, quizResult, psychometric-report): mutations `ADMIN`-only,
     scoped by `assertCanManage`/ownership as usual.
   If you're not sure which bucket a new resource falls into, ask rather than guess — a UI that lets
   ADMIN reach a write action the backend actually 403s is a real, previously-shipped bug here
   (silent failure, no error shown) — gate the frontend to match, don't just add the backend rule.

## Do NOT
- Return a raw `User` (leaks BCrypt `password`) or a raw `Question` to a non-admin (leaks `answer`).
  Map to a DTO with a static `from(entity)` factory — see `AdminDto`, `ExamQuestionDto`.
- Trust `createdBy`/owner ids from the request body.
- Add a `@ControllerAdvice`/custom exception type to "improve" error handling for one endpoint —
  the whole app uses `ResponseStatusException` only; stay consistent unless refactoring all of it.
- Dereference request-body relations without a null check (e.g. `question.getQuiz().getQuizId()`
  NPEs on a malformed body — guard it and throw `BAD_REQUEST`).

## Verify
`./mvnw test`, then drive the route with `curl` using a real Bearer token (log in via `/api/login`).
Confirm a second admin gets **403** on the first admin's resource — that's the ownership check working.
Remember: **kill the backend process and relaunch it** after any code change — devtools'
auto-restart is not reliable here (a plain `mvnw compile` does not reliably trigger it), so
"restart" means kill-port-8081-and-relaunch, not trusting the running process to pick up the diff.

## Log
After every backend change or live test, **append an entry to `.claude/testing-log.md`** (newest at
bottom, never rewrite past entries). Use the format documented at the top of that file:
`what` / `files` / `result` (verified how — include the 403 check and the curl evidence) /
`notes`. Read that log before starting so you inherit the current runtime facts (DB state, env vars,
what's already verified). Never write secrets — reference the env var name.
