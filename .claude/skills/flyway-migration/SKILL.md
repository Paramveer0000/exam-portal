---
name: flyway-migration
description: Add a Flyway schema migration to the Exam Portal backend. Use whenever a DB table/column changes — new entity field, new table, index, or backfill. Required because Hibernate runs ddl-auto=validate and will not create or alter schema.
---

# flyway-migration

Flyway **owns the schema**; Hibernate is `ddl-auto=validate` (`application.properties`). If an
entity field has no matching column, the backend **fails to boot** with a schema-validation error.
So any entity change needs a migration in the same commit.

## Add one

1. Create `exam-portal-backend/src/main/resources/db/migration/V<N>__<snake_desc>.sql`.
   `<N>` = next integer after the highest existing (`V1__baseline` … `V7__school_fields`).
   Double underscore after the version. Example: `V8__quiz_time_limit.sql`.

2. Write plain MySQL DDL. Follow the existing migrations' style:
   - Idempotent seeds use `INSERT IGNORE` (see `V2__seed_roles.sql`).
   - New nullable FK + index pattern is in `V4__ownership_and_timestamps.sql`.
   - Add an FK constraint explicitly when you want referential integrity (`V5` adds
     `quiz_results.user_id → users.user_id`).
   - One coherent change per file; keep the leading comment describing intent (repo convention).

3. Update the matching JPA entity in `models/` so field ↔ column line up. Watch the quirks:
   - Boolean columns: `User.isActive` uses normal `isActive()`; `Quiz.iActive` maps `is_active`
     with the odd `isIActive()` accessor — match whichever entity you touch, don't "fix" it here.
   - Ownership/user links are raw `Long` (`createdBy`, `QuizResult.userId`), not `@ManyToOne`.
   - `QuizResult.attemptDatetime` is a **String** column, not a temporal type.
   - DB-managed timestamps use `@Column(insertable=false, updatable=false)` (see `Category.createdAt`).

4. Boot the app (`./mvnw spring-boot:run`). Flyway applies `V<N>` on startup; the log shows
   `Migrating schema ... to version <N>`. Validation passing = entity and column agree.

## Notes
- `spring.flyway.baseline-on-migrate=true`, `baseline-version=1` — a pre-existing DB from the old
  `ddl-auto=update` era is stamped at V1 (V1 skipped) and V2+ applied. Don't renumber existing files.
- Never edit an already-applied migration; Flyway checksums them and will refuse to start.
  Fix-forward with a new `V<N+1>`.
- No down-migrations in this project.
