# Live Testing Log

Append-only record of events, changes, and findings during live testing of the Exam Portal.
**Purpose: an LLM reads this before acting** — it captures runtime state, known-good facts,
gotchas, and open issues that are NOT in the source code or git history.

## How to use this log (LLM instructions)
- **Read the whole file first** when resuming work — it tells you how the stack was last run,
  what's verified, and what's broken.
- **Append a new entry after every change or test** (code change, migration, feature, bug repro,
  config discovery). Newest at the bottom. Never rewrite past entries — correct them with a new one.
- **Entry format** (keep it compact):
  ```
  ### YYYY-MM-DD HH:MM — <short title>  [type: run|change|bug|finding|config]
  - what: <one line>
  - files: <paths touched, or "none">
  - result: <verified? how? pass/fail + evidence>
  - notes / follow-ups: <gotchas, open items>
  ```
- **Do not put secrets** (real passwords, tokens) in here — reference the env var name instead.
- Test data created during a session must be cleaned up; note the cleanup in the entry.

---

## Runtime facts (current, keep updated)
- **MySQL**: XAMPP at `C:\xampp\mysql\bin`, port 3306, user `root`, **empty password**
  (config default `root@` is wrong for this box — pass `--spring.datasource.password=`).
- **JAVA_HOME** must be set for `mvnw`: `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`.
- **Backend** `:8081` — `./mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password="`
  with `JWT_SECRET` set. **Restart required after backend code changes** (no auto-recompile via mvnw).
- **Frontend** `:3000` — `npm start`, hot-reloads on save.
- **DB is pre-existing/populated** (real data). 8 base users; super-admin boot-seed is SKIPPED
  because SUPER_ADMINs already exist (`suptest`, `dalveer`). Passwords are BCrypt — unknown.

---

## Entries

### 2026-07-19 17:05 — Brought stack up  [type: run]
- what: started MySQL (XAMPP) + backend :8081 + frontend :3000, smoke-tested.
- files: none.
- result: PASS. `GET /api/teachers` → 200 with real teachers. Login page renders; login POST
  round-trips (frontend→proxy→backend→DB) and surfaces 401 correctly.
- notes: needed JAVA_HOME (mvnw) and empty datasource password (XAMPP root). Both now in runtime facts.

### 2026-07-19 17:20 — "New DB row on update/delete" report  [type: bug]
- what: investigated claim that editing username / deleting creates a new row instead of updating.
- files: none (read-only).
- result: NOT REPRODUCIBLE on current code. Reproduced live: profile username change → row updated
  in place (count 9→9); admin delete → row removed cleanly (10→9), no leftover. Current services
  load-by-id then mutate+save; `User.userId` is IDENTITY, so save = UPDATE. Likely seen on an older build.
- notes: test users created and cleaned up; DB back to 8.

### 2026-07-19 17:45 — Feature: school can edit student profile  [type: change]
- what: added `PUT /api/students/{studentId}` and an inline Edit row on My Students page.
- files: StudentController.java, StudentService.java, StudentServiceImpl.java (backend);
  studentsServices.js, pages/admin/AdminStudentsPage.js (frontend). Reused `UpdateProfileRequest`.
- result: PASS, verified live: update in-place (no new row, 10→10), non-owner admin → 403,
  response is `StudentDto` (no password leak). Frontend compiles (only pre-existing useEffect warning).
- notes: `/api/students/**` already ADMIN-only in SecurityConfig (super admin via hierarchy) — no rule change.
  Throwaway teacher+student cleaned up (FK order: delete student before teacher); DB back to 8.

### 2026-07-19 18:20 — Stack restart (new session)  [type: run]
- what: all three tiers were down (previous session's processes stopped); brought stack back up.
- files: none.
- result: PASS. MySQL :3306 (XAMPP), backend :8081 (`GET /api/teachers` → 200 with data),
  frontend :3000 (HTTP 200). Used runtime facts above (JAVA_HOME, empty datasource password).
- notes: backend includes the student-edit feature from the previous entry (still uncommitted).

### 2026-07-19 20:45 — Feature: per-quiz exam timer (school-configurable)  [type: change]
- what: school toggles a timer on/off and sets duration in Add/Update Subject.
- files: V8__quiz_timer.sql (new: timer_enabled BIT, timer_minutes INT NULL); Quiz.java
  (timerEnabled/timerMinutes); QuizServiceImpl.java (validateTimer on add+update);
  AdminAddQuiz.js + AdminUpdateQuiz.js (switch + conditional minutes field).
- result: PASS, verified live. Flyway V8 applied on boot. Create with timer=on,30min → persisted
  (timerEnabled=true, timerMinutes=30). Timer=on with no minutes → 400. Toggle off via PUT →
  minutes cleared to null (validateTimer nulls it). Test data cleaned up; DB back to 8 users, 0 leftovers.
- notes: only backend-persist + admin config. Student-side countdown/enforcement during exam
  (UserQuestionsPage timer + auto-submit) NOT built — timerMinutes is exposed on the quiz for it.

### 2026-07-19 21:00 — Feature: student attempt progress bar  [type: change]
- what: progress bar on the exam page showing answered / total questions, updates as student picks answers.
- files: components/Question.js (new `onAnswered(quesId)` callback fired in saveAnswer);
  pages/users/UserQuestionsPage.js (ProgressBar, answeredCount state, recomputeAnswered reads
  localStorage.answers ∩ this quiz's quesIds; clears stale answers on fresh attempt).
- result: frontend compiles clean (only pre-existing useEffect-deps warning). Logic: count = questions
  with a saved answer in localStorage; bar = count/total * 100.
- notes: fixed a latent bug in passing — localStorage.answers now cleared at exam start, so a new
  attempt doesn't inherit a previous quiz's answers (was accumulating). Visual drive not run (no known
  student password to enter an exam); logic is trivial + compile-verified.

### 2026-07-19 21:15 — Fix: quiz manual instructions use real variables  [type: change]
- what: UserQuizManualPage showed wrong/unitless numbers ("submit within 0", arbitrary marks=q*5).
  Rewrote to real quiz fields.
- files: pages/users/UserQuizManualPage.js.
- result: compiles clean. Now: questionsCount = questionsPerExam ?? numOfQuestions; time limit from
  the timer feature (timerEnabled/timerMinutes, else "No time limit"); total marks = 1/question;
  passing % from quiz.passingPercentage. "Timer starts" line + auto-submit note only shown when timer on.
- notes: removed duplicate "practice purpose" bullets and the unitless `numOfQuestions*2` timer line.
  ("0 questions" for the asdf quiz is honest — that quiz genuinely has an empty pool.)

### 2026-07-19 21:35 — Feature: "all questions mandatory" toggle per quiz  [type: change]
- what: school toggles whether students must answer every question before submitting.
- files: V9__quiz_mandatory_questions.sql (all_questions_mandatory BIT NOT NULL DEFAULT 0);
  Quiz.java (allQuestionsMandatory); AdminAddQuiz.js + AdminUpdateQuiz.js (switch + payload);
  UserQuestionsPage.js (block manual submit when mandatory && answeredCount < total);
  UserQuizManualPage.js (instruction line reflects the setting).
- result: PASS. Flyway V9 applied. API round-trip: create with flag=true persisted True; PUT flag=false
  persisted False. Frontend compiles clean. Student manual-submit guard uses existing answeredCount.
- notes: enforcement is on the manual Submit button only; timer auto-submit still submits partial (can't
  block time-up). Pure boolean, no service validation needed. Test data cleaned; DB back to 8 users.

### 2026-07-19 21:50 — Fix: timeout auto-submit failed ("Quiz not Submitted")  [type: bug]
- what: on timer expiry the quiz showed the failure alert instead of submitting.
- files: pages/users/UserQuestionsPage.js.
- result: root cause — the timesUp branch called `submitQuiz(dispatch, userId, quizId, answers, token)`
  but the action signature is `submitQuiz(dispatch, quizId, answers, token)`. The extra `userId` shifted
  every arg (quizId←userId, answers←quizId, token←answers), so the POST went out with a wrong quizId and
  a bad auth header → 4xx → failure branch. Removed the stray `userId` arg to match the manual path. Compiles clean.
- notes: `userId` const is now unused in that file (harmless lint warning). Separate cosmetic issue: the
  alert prints quizTitle which can be the quizId ("12") when the exam is launched without a quizTitle URL param.

### 2026-07-19 22:05 — Feature: Exit Quiz button + highlight opened quizzes  [type: change]
- what: (1) "Exit Quiz" button on the exam page (leave without submitting, confirm first);
  (2) quizzes a student has opened once are highlighted (green border + "Opened" badge) in the panel.
- files: pages/users/UserQuestionsPage.js (exitQuizHandler + button; marks quizId opened on mount);
  pages/users/UserQuizzesPage.js (reads opened set, green border/badge on opened cards).
- result: frontend compiles clean. Opened set stored in localStorage key `openedQuizzes_<userId>`
  (string quizIds); marked when the exam questions page mounts.
- notes: `userId` const in UserQuestionsPage is now used again (opened-marking) — the earlier unused-var
  note no longer applies. Persistence is client-side (localStorage), wiped on logout (localStorage.clear).
  Cross-user safe via per-userId key. A backend "attempts opened" table would survive logout if needed.

### 2026-07-19 22:30 — Fix: exam auto-submitted instantly on Start ("Quiz not Submitted")  [type: bug]
- what: opening a quiz and clicking Start fired the failure alert immediately (esp. quiz "12", 1 question).
- files: pages/users/UserQuestionsPage.js.
- result: root cause — timer was broken two ways: (1) timeRemaining initialised to
  `questions.length*2*60`, but questions is EMPTY at mount → 0; (2) the countdown effect had empty deps
  and a stale closure, so its first tick saw timeRemaining=0 and called submitQuizHandler(true) ~1s after
  open. Rewrote the timer: useRef-based interval that starts ONCE after questions load, duration =
  school timer (timerEnabled/timerMinutes) else legacy 2 min/question, functional-updater decrements and
  fires a single guarded auto-submit at 0 (submittedRef). Also neutralised the second, conflicting
  ReactSpinnerTimer auto-submit (onLapInteraction → no-op; now visual only). Manual submit + Exit now stop
  the timer via stopTimer() and set the guard. Compiles clean.
- notes: builds on the earlier timesUp arg-order fix. Not browser-verified here (preview pane unresponsive);
  student aaaa/test1234 available for the user to confirm quiz 12 no longer instant-errors.

### 2026-07-19 22:55 — Feature: psychometric test + career-guidance platform (v1)  [type: change]
- what: platform is now psychometric-only. Questions carry a dimension (9 MI + 6 RIASEC); submit
  computes+persists a profile; report endpoint + printable report page with career guidance.
- files: V10__question_dimension.sql (backfill LOGICAL, NOT NULL), V11__psychometric_reports.sql
  (1 row/attempt, FK quiz_res_id, unique), V12__career_suggestions.sql (12 seeded fields).
  Backend: Question.dimension + validation (QuestionServiceImpl, uppercased, 15 codes);
  PsychometricReport + CareerSuggestion entities/repos; PsychometricReportService(+Impl) — Likert
  ordinal 1-4 per answer (label OR text resolved), MI share %, ranks, Mackenzie domains, RIASEC
  mean /10, quotients (IQ/EQ/AQ/CQ/SQ = documented MI-pair sums normalized to max=100), career
  ranking (weight x mean of driving-dimension scores, top 6, stars 1-5), assert self-check;
  scoreAndPersist hooked into QuizResultController.submitQuiz; GET /api/psychometric-report/{id}
  (PsychometricReportController), rule USER+ADMIN in SecurityConfig, per-row ownership in service.
  Frontend: psychometricReport slice (constants/service/action/reducer + store); report page
  /psychometricReport/:quizResId (all roles; MI table+bars, domains, RIASEC bands+Holland code,
  quotients, ranked careers with stars, window.print + @media print); "View Report" button in
  UserQuizResultPage; dimension select added to Add/Update Question forms (backend now 400s without).
- result: PASS end-to-end. V10-12 applied (schema at 12). Seeded quiz 17 with 15 tagged questions,
  submitted as zpsystudent → quizResId 7. Math verified: LOGICAL 4/19=21.1% rank1, domains sum 100,
  IQ normalized 100, Holland IRA, careers Engineering/Environment/Data top. Ownership verified:
  aaaa (other teacher's student) → 403; owning school reads OK. Report page rendered in browser
  with all sections. Frontend compiles clean.
- notes: security review of diff = no findings (no entity leaks, DTO-only out, denyAll intact).
  Seed data KEPT for demo: school zpsyschool, student zpsystudent (both pw known to user session),
  quiz 17 "ZPsyTest", report at /psychometricReport/7. assert self-check needs -ea to fire.
  Scoring rule: chosen option ordinal = Likert points (option1=1..option4=4).

### 2026-07-20 00:10 — Psychometric question banks + scoring validation  [type: change]
- what: seeded 3 tagged question banks (Class 6-8/9-10/11-12), validated scoring accuracy
  against known-profile fixtures, tuned career formula.
- files: docs/psychometric-sources.md (construct citations), seed/psychometric/*.json (3 banks,
  60 items each, all 15 dims x4, original age-scaled Likert items), seed/psychometric/seed-banks.ps1
  (idempotent API seeder -> seeded-index.json), seed/psychometric/validate-scoring.ps1 (fixtures),
  docs/psychometric-validation.md (18/18 results), PsychometricReportServiceImpl.rankCareers (fix).
- result: PASS 18/18. Seeded quizzes 19/20/21 under demo school psychobank (student psychobankstudent,
  both pw seedpass123). Verified DB: each quiz 60 items, 15 dims x4. Fixtures: MI spikes
  (LOGICAL/VERBAL/INTERPERSONAL/KINESTHETIC) -> correct rank1 + quotient + careers; RIASEC R spike
  -> R dominant, Holland RIA, hands-on careers; sum sanity (MI~100, domains fixed sums, ranks 1..9
  perm); boundary (uniform -> even, no crash).
- notes: FOUND + FIXED a scoring quirk — career ranking averaged MI shares (~10-20) with RIASEC
  mapped to 0-100, so RIASEC-driven fields dominated even MI spikes (KINESTHETIC spike wrongly topped
  Engineering). Fixed to score each dimension by prominence within its own system (value/system-avg).
  Documented in docs/psychometric-validation.md. Re-seed: run seed-banks.ps1; re-validate: validate-scoring.ps1.
  Item wording original (public MI/RIASEC constructs, cited); no proprietary items copied.
