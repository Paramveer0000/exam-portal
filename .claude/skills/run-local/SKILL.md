---
name: run-local
description: Bring up the Exam Portal locally — MySQL, Spring Boot backend (:8081), React frontend (:3000) — and smoke-test that the stack is wired. Use when asked to run, start, boot, or verify the app end-to-end.
---

# run-local

Start order matters: **MySQL → backend → frontend**. Backend fails fast at boot if MySQL
is down (Flyway can't connect) or if it's on the wrong JDK.

## 0. Preconditions

- **JDK 17** — not 25. Lombok 1.18.38 is pinned for 17; a newer JDK breaks the build.
  Check: `java -version`.
- **MySQL on `:3306`**. The DB `exam-portal` is auto-created (`createDatabaseIfNotExist=true`),
  so the server only needs to be up with credentials that match — config default is `root` / `root@`,
  but **on this box it's XAMPP MySQL (`C:\xampp\mysql\bin`) with an empty root password** — pass
  `--spring.datasource.password=` explicitly (see command below). Don't assume `root@` works;
  verify with `mysql -u root -h 127.0.0.1 -e "SELECT 1"` before chasing a Flyway connection error.
  Check port: `powershell "Test-NetConnection localhost -Port 3306 -InformationLevel Quiet"`.

If MySQL isn't running, start the local service (do not guess a password — ask the user if the
default `root@` doesn't connect). Never hard-delete or reset an existing `exam-portal` schema.

## 1. Backend

```bash
cd exam-portal-backend
export JWT_SECRET=local-dev-secret          # any non-empty value; silent public fallback otherwise
export SUPERADMIN_PASSWORD=change-me         # only if you need the first SUPER_ADMIN seeded
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password="   # empty XAMPP root pw
```
On Windows PowerShell use `$env:JWT_SECRET="..."` etc. Flyway applies pending `V*` migrations on
startup (highest applied so far: check `db/migration/` for the current max); watch the log for
`Successfully applied` and `Started ExamPortalBackendApplication`.

**After any backend code change you must kill the process and relaunch — do not rely on
spring-boot-devtools to auto-restart.** In this environment devtools did not pick up plain
`mvnw compile` runs; a stale JAR kept serving old behavior for several restarts in a row, which read
as a phantom bug (a real fix looked like it wasn't taking effect at all). Always:
```bash
# find + kill whatever holds :8081, then relaunch with the command above
powershell -Command "$c = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue; if ($c) { Stop-Process -Id $c.OwningProcess -Force }"
```
Then confirm `Started ExamPortalBackendApplication` appears again in the fresh log before testing.

Smoke-test (public endpoint, no auth):
```bash
curl -s http://localhost:8081/api/teachers        # returns [] or a JSON list of teachers
```

## 2. Frontend

```bash
cd exam-portal-frontend
npm install --legacy-peer-deps      # required — plain install hits peer-dep conflicts
npm start                            # CRA dev server on :3000, proxies /api → 127.0.0.1:8081
```

## 3. Drive it

Open `http://localhost:3000`, register a school/admin or log in as the seeded super admin. Category/
quiz/question content is **SUPER_ADMIN-only** now (a plain ADMIN school account can only read it,
see `security-review`) — to create/edit content you need a SUPER_ADMIN session, not a school login.
A blank page = check the browser console and the backend log. If `/api` calls 404 in the browser but
`curl` to `:8081` works, the CRA proxy (`src/setupProxy.js`) isn't running — you're hitting the built
bundle, not `npm start`.

## Common failures
- `Flyway ... Unable to obtain connection` → MySQL down or wrong creds.
- `Schema validation: missing table/column` → an entity was changed without a migration; see `flyway-migration`.
- Backend boots but every authed call is 401/403 → `JWT_SECRET` differs from the one that signed the token; re-login.
