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
   with **no rule is blocked**. Match the existing style: GETs open to `USER`+`ADMIN`,
   mutations `ADMIN`-only, admin-management under SUPER_ADMIN. `RoleHierarchy` already makes
   SUPER_ADMIN ⊇ ADMIN, so an `hasAuthority("ADMIN")` rule also admits super admins.

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
Remember: **restart the backend** after any code change (`mvnw spring-boot:run` doesn't auto-recompile).

## Log
After every backend change or live test, **append an entry to `.claude/testing-log.md`** (newest at
bottom, never rewrite past entries). Use the format documented at the top of that file:
`what` / `files` / `result` (verified how — include the 403 check and the curl evidence) /
`notes`. Read that log before starting so you inherit the current runtime facts (DB state, env vars,
what's already verified). Never write secrets — reference the env var name.
