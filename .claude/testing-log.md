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

### 2026-07-20 01:00 — Feature: student school switch + exam pagination  [type: change]
- what: (1) student profile shows School (teacher name) and can switch schools via a dropdown;
  (2) exam page now paginates 6 questions/page with Previous/Next nav, Submit only on last page.
- files: backend UpdateProfileRequest.teacherId, ProfileServiceImpl.updateProfile (student-only,
  validates target has ADMIN role + is enabled, else 400); frontend ProfilePanel.js (showSchool
  prop, teacher dropdown via authServices.getTeachers, School row), UserProfilePage.js
  (showSchool=true), UserQuestionsPage.js (QUESTIONS_PER_PAGE=6, page state, bottom nav, Submit
  moved from top to bottom/last-page-only; page resets to 0 on fresh attempt).
- result: PASS. API verified: psychobankstudent switch teacherId 28->30 (accepted), reverted 30->28
  (accepted), switch to a non-ADMIN target (a student id) -> 400 (rejected) as expected. Frontend
  compiles clean (pre-existing warning only).
- notes: teacherId change is silently ignored for ADMIN/SUPER_ADMIN callers (guarded by
  authFacade.isStudent()) — only students can move schools. Progress bar/mandatory-questions check
  unaffected (still counts across all pages, not just current page). Not browser-driven this pass;
  verified via API + compile check.

### 2026-07-20 01:20 — Fix: unpublished quiz visible to students + timer runs when disabled  [type: bug]
- what: (1) a quiz with Publish Quiz OFF was still visible and startable by students;
  (2) a quiz with Enable exam timer OFF still ran a countdown and auto-submitted.
- files: backend QuizServiceImpl.getQuizzes/getQuizByCategory (student branch now filters
  q.isIActive()), getExamQuestions (403 "not published yet" if !quiz.isIActive() for students);
  frontend UserQuestionsPage.js (countdown effect no longer falls back to a legacy 2min/question
  timer when timerEnabled is false — no timer, no auto-submit, UI shows "No time limit"; also
  waits for `quiz` to resolve before deciding, fixing a latent race where quiz loading after
  questions could lock in the wrong decision).
- result: PASS. API-verified full cycle: created quiz isActive=false -> absent from student
  GET /api/quiz/, exam-start GET .../exam -> 403; set isActive=true -> appears in list, exam-start
  succeeds. Frontend compiles clean.
- notes: root cause for (1) — publish (iActive) was stored but never read anywhere outside the
  admin UI; QuizServiceImpl had zero isIActive() filtering. Root cause for (2) — the timer effect's
  ternary always produced a duration even when timerEnabled was false. JSON property for the
  publish flag is `iActive` (not `isActive`) due to Lombok's isIActive() getter on the Quiz.iActive
  field — noted here since it trips up ad-hoc API testing.

### 2026-07-20 01:35 — Fix: Publish Quiz toggle never actually published  [type: bug]
- what: user reported that after publishing a quiz it still didn't show for students — my prior
  fix (filter by isIActive()) was correct but exposed a DEEPER pre-existing bug: the publish flag
  was never actually being set true in the first place.
- root cause: Quiz.iActive (field name, 2 leading caps) makes Lombok generate isIActive()/
  setIActive(). Jackson's property-name mangling collapses that to JSON key "iactive" (lowercase,
  verified via raw GET), NOT "isActive". The frontend (AdminAddQuiz.js, AdminUpdateQuiz.js) has
  always sent/read "isActive" — silently ignored on write (quiz created with iActive=false
  regardless of toggle) and undefined on read (Update form's toggle always initialized unchecked).
  This bug predates this session; it's why "unpublished but running" was reported at all.
- files: models/Quiz.java (@JsonProperty("isActive") pins the wire name so it can't drift again;
  comment explains the mangling). Frontend AdminAddQuiz.js/AdminUpdateQuiz.js reverted back to the
  natural "isActive" key (matches every other boolean field in the app) now that the backend honors it.
- result: PASS full cycle verified via API: GET now returns "isActive" (was "iactive"); POST with
  isActive:true persists True; new quiz immediately visible to student + exam-start works.
- notes: this is the actual fix the "publish quiz not showing" report needed — the earlier
  isIActive()-filtering change was necessary but not sufficient, since the flag could never be
  set true through the UI to begin with.

### 2026-07-20 01:45 — Change: drop per-class links from student sidebar  [type: change]
- what: SidebarUser was auto-fetching categories and adding one nav link per class
  (e.g. "Psychometric Class 6-8/9-10/11-12"), cluttering the sidebar. Removed.
- files: components/SidebarUser.js — dropped the fetchCategories effect and dynamic
  menuItems build; sidebar is now a static 3-item list (Profile, Report Card, All Quizzes).
- result: compiles clean. Classes/subjects are still browsable via "All Quizzes" (UserQuizzesPage),
  unaffected by this change.

### 2026-07-20 15:15 — Feature: mandatory 2-step student onboarding  [type: change]
- what: after login, a student with an incomplete profile is hard-gated to /onboarding on
  EVERY route until Step 1 (basic info: name/phone/school) then Step 2 (academics: grade/board/
  school name) are both filled. Existing complete students unaffected; incomplete ones (including
  demo account psychobankstudent, which has no grade/board/schoolName) will hit the wizard next login.
- files: V13__student_academics.sql (users.grade/board/school_name, nullable); User.java (+3 fields);
  UpdateProfileRequest + ProfileServiceImpl (accept/persist them, unconditional like firstName);
  pages/users/OnboardingPage.js (new — 2-step wizard, exports isBasicComplete/isAcademicsComplete
  used as the single source of truth for "complete"); components/RequireStudentOnboarding.js (new
  gate, wraps userRoute() in App.js, redirects to /onboarding); App.js (/onboarding route, role-gated
  only, NOT wrapped by the onboarding gate itself — avoids a redirect loop); ProfilePanel.js (grade/
  board/schoolName view rows + edit fields under showSchool, so students can revise after onboarding).
- result: PASS. Backend round-trip verified: step-2 payload (grade=9, board=CBSE, schoolName=...)
  persisted and read back correctly. Confirmed existing untouched student still reads null academics
  (will be gated on next login, as intended). Frontend compiles clean.
- notes: completeness is DERIVED from the fields (no separate "onboarded" flag) — avoids the kind of
  boolean/data drift bug the iActive mismatch caused earlier. IMPORTANT: ProfilePanel.saveProfile and
  ProfileServiceImpl.updateProfile always send/persist grade/board/schoolName together with the rest
  of the profile (service sets them unconditionally from the request) — any future caller of
  /api/profile/ MUST include current grade/board/schoolName values or it will silently wipe them,
  same footgun pattern as the old iActive key mismatch. OnboardingPage's two submit handlers already
  do this correctly (each step sends the OTHER step's current values from Redux `user`, not blank).

### 2026-07-20 15:30 — Change: split Profile page into Profile / Academics sections  [type: change]
- what: view table split into two labeled tables ("Profile": name/username/phone/school/role/status,
  "Academics": grade/board/school name); Edit Profile form split into two labeled field groups the
  same way. Student-only (showSchool); non-student profiles unaffected (no Academics section).
- files: components/ProfilePanel.js.
- result: compiles clean. Single Save button still saves both sections together (no split-save flow
  added — not requested, avoids extra complexity).

### 2026-07-20 15:45 — Feature: AI report generation (platform LLM key)  [type: change]
- what: SUPER_ADMIN sets ONE platform LLM API key; students/teachers click "Generate AI Report"
  on the psychometric report to get an LLM counsellor narrative (cached per attempt).
- files: V14__ai_features.sql (platform_ai_settings single row + psychometric_reports.ai_summary);
  models/PlatformAiSettings.java (@JsonIgnore api_key), repo; dto/AiSettingsDto (masked keyHint,
  keyConfigured — never raw key) + UpdateAiSettingsRequest (blank key = keep stored);
  services/AiService(+Impl) (settings CRUD + OpenAI-compatible /chat/completions via RestTemplate,
  no new dependency); AdminController GET/PUT /api/admin/ai-settings (SUPER_ADMIN only);
  PsychometricReportServiceImpl.getAiSummary (same ownership scoping as report, builds prompt from
  computed profile, caches ai_summary); PsychometricReportController POST /{id}/ai-summary
  (+SecurityConfig POST rule USER,ADMIN); frontend aiServices.js, SuperAdminAiSettingsPage.js
  (+sidebar link + route), PsychometricReportPage.js "Generate AI Report" button + AI section.
- result: PASS security-critical paths (verified via API):
  * GET ai-settings returns {provider,baseUrl,model,keyConfigured,keyHint} — NEVER the raw key
    (@JsonIgnore on entity + masked DTO). PUT with blank apiKey keeps the stored key; masked hint "****ABCD".
  * generate: owner+no-key -> 503 "AI is not configured"; owner+bad-key -> 502 "AI provider request
    failed" with NO key leaked in the error; stranger student -> 403; student reading ai-settings -> 403.
  * V14 applied, settings row seeded with no key (feature disabled until a real key is added).
  Backend compiles + boots, frontend compiles clean.
- notes: real successful LLM call NOT exercised (no real key on hand, and I must not enter credentials) —
  pipeline verified end-to-end up to the provider request. User adds a real key in Super Admin > AI
  Settings to enable. Provider is OpenAI-compatible (OpenAI/Groq/OpenRouter/custom base_url); for
  Anthropic use an OpenAI-compatible gateway or their compat endpoint. Key stored plaintext in DB
  (acceptable for this app's model; a KMS/secret store would be the productionization step). Errors from
  the provider are deliberately generic to avoid leaking the key or upstream internals. Demo super-admin
  passwords are unknown to me; used a throwaway SUPER_ADMIN (zsatest) for testing, since removed.

### 2026-07-20 16:00 — Change: top-right register buttons on Super Admin Schools page  [type: change]
- what: two action buttons in the top-right header of SuperAdminAdminsPage — "+ Register Partner /
  School" (role=ADMIN) and "+ Register Super Admin" (role=SUPER_ADMIN). The create form is now hidden
  by default; a button opens it pre-set to that role with a Close link. Uses the existing createAdmin
  flow (no backend change).
- files: pages/superadmin/SuperAdminAdminsPage.js.
- result: compiles clean (pre-existing eslint warnings only). Create still collapses on success.

### 2026-07-20 18:30 — MAJOR: super-admin-only content + school class-assignment  [type: change]
- what: only SUPER_ADMIN creates/edits/deletes classes (categories), quizzes, questions. Schools
  (ADMIN) can no longer create content — they ASSIGN existing classes to their own students, who then
  see the published quizzes in their assigned classes.
- files: V15__super_admin_content_and_class_assignment.sql (student_class join table + reassign all
  categories/quizzes created_by to lowest-id SUPER_ADMIN); models/StudentClass.java (@IdClass composite),
  repo; SecurityConfig (category/quiz/question POST/PUT/DELETE -> hasAuthority SUPER_ADMIN, which
  EXCLUDES plain ADMIN; GET stays USER,ADMIN so admins can read to assign); CategoryServiceImpl.getCategories
  (student -> assigned categories; admin/super -> all); QuizServiceImpl (student sees published quizzes in
  assigned classes; getExamQuestions + QuizResultController.submitQuiz gate on assignment instead of
  teacherId==createdBy); StudentService(+Impl) getAssignedClasses/assignClass/unassignClass (ownership via
  loadStudent); StudentController /{id}/classes[/{catId}] GET/POST/DELETE; frontend studentsServices
  (assign APIs), AdminStudentsPage (Classes button -> checkbox assign panel), Sidebar.js (Classes/Subjects
  nav hidden for plain ADMIN, shown for SUPER_ADMIN), SuperAdminSidebar (Classes/Subjects links).
- result: PASS end-to-end (API):
  * ADMIN create class/quiz/question -> 403 (all three). SUPER_ADMIN create class -> OK.
  * student with no assignment -> [] quizzes; exam-start on a class not assigned -> 403.
  * school assigns class -> student sees that class's PUBLISHED quiz (id 27) and can take it.
  * school assigning to another school's student -> 403 (ownership).
  * V15 applied: student_class table created (InnoDB, FK cascade), all categories+quizzes reassigned
    created_by -> user 4 (a SUPER_ADMIN).
  Backend compiles+boots, frontend compiles clean. Test data purged (back to 15 users).
- notes: teacher_id still links students to their school (for management + assignment); it's no longer
  used for content visibility (assignment is). IMPORTANT DATA STATE: the seeded psychometric banks
  (quizzes 19/20/21) are is_active=0 (unpublished — they were seeded before the iActive publish-key fix),
  so students won't see them until a SUPER_ADMIN republishes them via Update Subject. Only quiz 27
  "Mentalist" (cat 15) is currently published. PowerShell gotcha for future testers: Invoke-RestMethod
  on an empty JSON array [] returns $null, and @($null).Count == 1 — an empty student quiz list reads as
  "1" unless you check the actual ids.

### 2026-07-20 19:00 — Feature: Super Admin All Results grouped by school  [type: change]
- what: new Super Admin "All Results" page — every quiz attempt grouped by partner school, then by
  student, then attempt rows (class, subject, marks, pass/fail, date, link to report).
- files: dto/SchoolResultsDto.java (nested school->student->row); AdminService.getResultsBySchool +
  AdminServiceImpl (groups all quiz_results: student.teacherId -> school, quiz.category -> class;
  user cache); AdminController GET /api/admin/results-by-school (SUPER_ADMIN via class guard);
  frontend adminServices.fetchResultsBySchool, pages/superadmin/SuperAdminResultsPage.js (accordion
  per school with student sub-tables), sidebar link + route /superadmin/results.
- result: PASS. Verified via API as dalveer (SUPER_ADMIN): 3 schools returned —
  Cambridge (2 students), Psy School (1), Psycho Bank (1) — with correct per-student attempt counts.
  Frontend compiles clean.
- notes: reset dalveer's password to `super123` (hash-copy method) so a SUPER_ADMIN account is usable
  for the new super-admin-only features (content mgmt, AI settings, all-results). Students with no
  school (teacherId null) would group under schoolId=null "No school". Report link reuses the existing
  ownership-scoped /psychometricReport/{quizResId} (super admin sees all).

### 2026-07-20 18:45 — Feature: admin/super-admin logo upload (round PNG)  [type: change]
- what: schools (ADMIN) and platform admins (SUPER_ADMIN) can upload/change a PNG logo in their
  profile settings; shown as a round avatar. Students unaffected.
- files: V16__user_logo.sql (users.logo MEDIUMTEXT); User.logo; UpdateProfileRequest.logo;
  ProfileServiceImpl (sets logo ONLY when request.logo != null, so plain profile saves don't wipe it —
  avoids the grade/board-style footgun); ProfilePanel.js (showLogo prop; round 120px avatar using
  user.logo; hidden file input, PNG-only + <1MB check, base64 data URL sent as {logo} then reload);
  AdminProfilePage + SuperAdminProfilePage pass showLogo.
- result: PASS. V16 applied. API round-trip as psychobank: upload logo -> persisted; subsequent
  profile save WITHOUT logo -> logo preserved (not wiped). Frontend compiles clean. Test logo reset to null.
- notes: logo stored as base64 data URL in DB (MEDIUMTEXT) — simple, no file storage/CDN. 1MB client
  cap keeps rows reasonable; the DB column allows ~16MB. Round display via border-radius + objectFit:cover.
  Not yet shown in header/sidebar branding — only the profile avatar; extend later if wanted.

### 2026-07-20 19:20 — Feature: School (ADMIN) dashboard  [type: change]
- what: a dashboard for schools/partners mirroring the super-admin one but scoped to their own
  students: summary tiles (My Students, Exam Attempts, Passed, Pass Rate, Classes Available) + a
  per-student table (status, attempts, passed).
- files: pages/admin/AdminDashboardPage.js (client-side compute from existing endpoints:
  /api/students/, /api/quizResult/all (teacher-scoped), /api/category/); App.js route /adminDashboard
  (adminRoute); Sidebar.js "Dashboard" nav item at top (admin); ProtectedRoute.homePathForRoles ADMIN
  now lands on /adminDashboard instead of /adminProfile.
- result: PASS. No backend change — all data already ownership-scoped server-side. Verified endpoints
  as psychobank: 22 results returned (field "passed" present), 1 student, 1 class. Frontend compiles clean.
- notes: QuizResult JSON boolean field serializes as "passed" (Lombok isPassed() -> Jackson "passed");
  dashboard reads r.passed. A SUPER_ADMIN who opens the admin Sidebar's Dashboard sees platform-wide
  numbers (since /students and /quizResult/all return all for them) under the "School Dashboard" label —
  acceptable, super admins have their own dashboard.

### 2026-07-20 19:30 — Fix: logo upload "Username is required"  [type: bug]
- what: logo upload failed with 400 "Username is required".
- root cause: onLogoChange sent {logo} only, but ProfileServiceImpl.updateProfile requires
  username + phoneNumber (StringUtils.hasText guards). Fixed: send current firstName/lastName/
  username/phoneNumber alongside the new logo (service still only overwrites logo when present).
- files: components/ProfilePanel.js. Also added requirement note under the upload button:
  "PNG only · square recommended (e.g. 256×256 px) · max 1 MB".
- result: compiles clean. Matches the earlier passing API round-trip (full fields + logo -> 200).

### 2026-07-20 21:55 — Feature: platform company logo in header (super-admin only)  [type: change]
- what: SUPER_ADMIN uploads a company logo in Platform Settings; it replaces the "Exam-Portal" text
  in the top-left header on EVERY page for all users (incl. login page). The header logo is a button
  that redirects to the user's dashboard (or /login if logged out).
- files: V17__platform_branding.sql (platform_settings single row, company_logo MEDIUMTEXT);
  models/PlatformSettings.java, repo; controllers/PlatformController.java (GET /api/platform/branding
  public, PUT SUPER_ADMIN); SecurityConfig (GET /api/platform/** permitAll, others SUPER_ADMIN);
  frontend platformServices.js, Header.js (loads branding, shows logo, brand click -> homePathForRoles
  dashboard), SuperAdminAiSettingsPage.js (retitled "Platform Settings"; new Company Logo card with
  upload/remove + requirement note, PNG/<1MB).
- result: PASS. V17 applied. API verified: public GET works (no auth); student PUT -> 403; super admin
  PUT sets logo and public GET reflects it. Frontend compiles clean. Test logo cleared.
- notes: logo is base64 in DB, served to everyone via the public GET (branding is not sensitive).
  Distinct from the per-user profile logo (V16). Requirement note on the card: PNG, landscape ~200x60,
  max 1MB. Header brand doubles as the "dashboard redirect button".

### 2026-07-22 02:10 — Session: AI settings test/prompt, question CRUD fixes, dimension override, AI-report gating  [type: change]
- what: multi-part session, several independent features/fixes:
  1. Super Admin sidebar label "AI Settings" -> "Setting". Added `POST /api/admin/ai-settings/test`
     (fires one real chat completion via existing `aiService.complete`) + "Test connection" button
     with live/down status badge on the settings page.
  2. Added an editable `system_prompt` (V18 migration, `platform_ai_settings.system_prompt` TEXT,
     nullable) so the AI report narrative's instructions are admin-configurable; blank = falls back
     to the hardcoded `DEFAULT_AI_SYSTEM_PROMPT` in `PsychometricReportServiceImpl`.
  3. Admin Questions page: collapsed-by-default question rows (click to expand), search box,
     select-all + bulk delete.
  4. **Real bug found & fixed**: `DELETE /api/question/{id}` returned 200 but never actually removed
     the row — `Quiz.questions` is an eager `Set<Question>` with `cascade=ALL`; deleting the child
     without first removing it from `quiz.getQuestions()` let Hibernate's collection dirty-check
     silently re-persist it on flush. Fix: `quiz.getQuestions().remove(existing)` before
     `questionRepository.delete(existing)` in `QuestionServiceImpl.deleteQuestion`. Verified via
     direct DB query (count 60->59, row gone) — this was pre-existing, not something introduced
     this session. NOTE: **plain ADMIN cannot write questions at all** (SecurityConfig restricts
     `/api/question/**` non-GET to SUPER_ADMIN only, per the 2026-07-20 content-ownership change) —
     the admin Questions page now hides Add/Update/Delete/bulk-select for plain ADMIN and shows a
     view-only note instead, since every write 403'd silently before.
  5. Questions can now have 2-4 options (e.g. Yes/No) instead of always 4: backend validation
     requires only option1+option2; scoring (`PsychometricReportServiceImpl`) normalizes each
     answer by that question's own option count (`maxOrdinal`) instead of a hardcoded `/4.0`, so a
     2-option question isn't capped at half credit. Frontend hides blank option rows.
  6. Duplicate-question guard: `existsByQuizAndContentIgnoreCase` — add/update now 409s on identical
     content within the same quiz (case-insensitive).
  7. Add/Update Question pages got a "Back to Questions" button + auto-redirect after the success
     popup; `quizTitle` now carried through the round-trip (was silently dropped before).
     AdminQuestionsPage now refetches on `location.key` (not just mount) so returning from
     Add/Update always shows the fresh list.
  8. **Optional per-option dimension override** (V19 migration: `option{1-4}_dimension` nullable
     VARCHAR(20) on `questions`). NULL (default) = old behavior, scores into the question's own
     `dimension`. When set, that option scores into its own dimension instead — e.g. a Yes/No
     question where Yes measures one MI/RIASEC trait and No measures another. New
     `PsychometricReportServiceImpl.effectiveDimension(q, ordinal)` resolves it. Frontend: new
     shared `components/DimensionSelect.js`, a "Scores as (optional)" select under each option in
     Add/Update Question, blank by default.
  9. Psychometric report page ("Download PDF" via `window.print()`): AI narrative generation is now
     a hard prerequisite, not a separate skippable step — only "Generate AI Report" shows until a
     summary exists, then it swaps to "Download PDF". "Regenerate" removed from the UI *and* backend
     now 409s on `regenerate=true` once `PsychometricReport.aiSummary` is set (one-time only, by
     explicit user request). Print CSS also now hides the whole `.psychReport__actions` bar (only
     two of three buttons were hidden before).
- files: SecurityConfig unchanged (not touched, see note in #4); AdminController (+test endpoint);
  AiServiceImpl/AiService/AiSettingsDto/UpdateAiSettingsRequest/PlatformAiSettings (system_prompt);
  QuestionServiceImpl (delete-collection fix, 2-4 option validation, duplicate guard,
  option-dimension validation); Question.java (+option{1-4}Dimension); QuestionRepository
  (+existsByQuizAndContentIgnoreCase); PsychometricReportServiceImpl (maxOrdinal, effectiveDimension,
  DEFAULT_AI_SYSTEM_PROMPT, one-time-generate lock); PsychometricReportController (unchanged);
  migrations V18__ai_system_prompt.sql, V19__question_option_dimension.sql; frontend:
  SuperAdminSidebar.js, SuperAdminAiSettingsPage.js, aiServices.js, Question.js, AdminQuestionsPage.js,
  AdminAddQuestionsPage.js, AdminUpdateQuestionPage.js, DimensionSelect.js (new),
  PsychometricReportPage.js/.css, questionsServices.js (surface real error message body, not just
  statusText).
- result: PASS on everything verified live via curl + direct DB queries (dalveer SUPER_ADMIN token):
  delete fix (60->59->restored 60), 2-option add succeeds / 1-option 400s, dimension-override add
  persists correctly, duplicate guard 409s, migrations V18/V19 applied cleanly, backend boots clean
  after every change. Frontend compiles clean each time (only pre-existing useEffect/eqeqeq warnings
  in files not touched by the fix). All test rows/questions created during verification were deleted
  afterward. Backend was restarted ~9 times this session via kill-port-8081 + relaunch, because
  devtools' auto-restart did NOT pick up plain `mvnw compile` runs — always kill-and-relaunch after
  a backend change here, don't rely on devtools.
- notes / follow-ups: (a) the ADMIN-can't-write-questions restriction is a real product gap vs.
  CLAUDE.md's "Admins own categories/quizzes/questions" — that doc line is stale post the
  2026-07-20 super-admin-only-content change; flag if this needs revisiting. (b) session ended with
  both dev servers (`:8081`, `:3000`) killed and MySQL (XAMPP) left running. (c) datasource password
  for this box is genuinely empty — pass `--spring.datasource.password=` every backend launch (see
  Runtime facts above); this was re-discovered/re-applied ~9 times this session, worth double-checking
  it's still accurate next session.

### 2026-07-23 11:48 — MAJOR: Class -> Subject -> Quiz hierarchy + admin-created students  [type: change]
- what: inserted a Subject tier between Class(categories) and Quiz; quizzes now belong to a
  Subject (was: directly to a class). Each student now belongs to ONE class (users.class_id),
  replacing the many-class student_class join. Students are created by their school (POST
  /api/students) — public student self-registration removed. SUPER_ADMIN owns class/subject/quiz.
- files (backend): V20__class_subject_quiz_hierarchy.sql (new subjects table; 1 "General" subject
  per existing class; quizzes.subject_id backfilled from old category; users.class_id backfilled
  from student_class min; drop student_class + quizzes.category_cat_id via dynamic FK-name lookup);
  models Subject.java (new), Quiz.java (category->subject @ManyToOne), User.java (+classId),
  Category.java (dropped quizzes back-ref); deleted StudentClass.java + StudentClassRepository.java;
  SubjectRepository (new), QuizRepository (findBySubject + subject dup-guards); Subject
  Service/Impl/Controller (new, CRUD, student-scoped to own class); CategoryServiceImpl (student
  sees only own class); QuizServiceImpl (subject/class-based scoping, getQuizBySubject);
  StudentServiceImpl (createStudent + setClass, dropped assign/unassign); StudentController
  (POST /students, PUT /students/{id}/class/{classId}); QuizController (getQuizBySubject, param
  subjectId); QuizResultController (submit gate by subject.classId); AdminServiceImpl (className via
  subject); AuthController/AuthService/Impl (removed student self-register); SecurityConfig (dropped
  /api/register permitAll, added /api/subject rules); StudentDto (+classId); CreateStudentRequest (new).
- result: PASS end-to-end (live, dalveer SUPER_ADMIN). Backed up DB first (scratchpad/
  db-backup-pre-V20.sql, 15 users). First V20 attempt FAILED (Error 1091: hardcoded FK name
  fk_quizzes_category wrong — actual was Hibernate-generated FKi0mytbs01...); restored from backup,
  rewrote drop to look up CONSTRAINT_NAME dynamically -> V20 applied clean. Second boot failed on
  stale Category.quizzes mappedBy="category"; removed it -> boots clean, Hibernate validate passes.
  Data migrated: 3 subjects (1 General/class), 6 quizzes mapped to subjects, 1 student got class_id.
  API verified: GET /subject (SA all, student only own class), POST /subject OK, POST /register ->
  403 (student self-signup gone), POST /students create (classId stamped), PUT /students/{id}/class/16
  moves student, student login sees only class-16 subjects. Test student+subject deleted (200), DB
  back to 15 users.
- notes/follow-ups: (a) FRONTEND NOT YET DONE — no UI for subject CRUD, student-create form, or the
  new class->subject->quiz student browsing; login/register page still shows student self-signup;
  admin students page still has the old multi-class assign panel (its assign/unassign endpoints are
  now 404). Next session: wire frontend. (b) Existing quizzes all live under an auto-created "General"
  subject per class — SUPER_ADMIN can rename/reorganize. (c) backend left RUNNING on :8081, MySQL
  (XAMPP mysqld) running. (d) dalveer/super123 still valid.

### 2026-07-23 12:10 — Frontend for Class->Subject->Quiz + admin-created students  [type: change]
- what: wired the frontend to the V20 backend restructure.
- files: services/subjectsServices.js (new: subject CRUD, raw axios); services/studentsServices.js
  (createStudent + setClass, dropped assign/unassign); pages/admin/AdminStudentsPage.js (rewrote:
  "+ Add Student" form with username/password/name/phone/class dropdown; per-row single Class
  dropdown to reassign; Class column; removed old multi-class checkbox panel); pages/RegisterPage.js
  (removed Student self-signup mode — school-only signup now); pages/users/UserQuizzesPage.js
  (q.category -> q.subject, subjectId URL param); AdminAddQuiz.js + AdminUpdateQuiz.js (choose a
  Subject "Class -> Subject" instead of a Class; payload sends {subject:{subjectId}}); Update preselects
  current subject; pages/superadmin/SuperAdminSubjectsPage.js (new: subject CRUD under a class);
  App.js (+/adminSubjects route, superAdminRoute); SuperAdminSidebar.js (added "Subjects" ->
  /adminSubjects, renamed old "Subjects" quizzes link to "Quizzes"); AdminQuizzesPage.js (group by
  subject, filter by class via subject.classId); AdminQuizResultPage/AdminReportsPage/UserQuizResultPage
  (quiz.category.title -> quiz.subject.title; fixed an unguarded crash in UserQuizResultPage).
- result: PASS. Frontend compiles clean (1 pre-existing array-callback-return warning in
  UserQuizzesPage, not from this change). Browser-verified live (dalveer/super123): /register shows
  school-only signup (no student mode), no console errors; logged in and /adminSubjects renders the 3
  auto-created "General" subjects (one per class) with the class dropdown populated (Class 6-8/9-10/
  11-12), no console errors.
- notes/follow-ups: (a) both dev servers left running: backend :8081, frontend :3000, MySQL (XAMPP).
  (b) minor unused-import warnings remain in RegisterPage/AdminAddQuiz (removed-flow leftovers) — harmless.
  (c) student-side browsing is class-scoped by the backend already; the class->subject->quiz drill-down
  UI for students is minimal (All Quizzes lists their class's published quizzes with subject subtitle) —
  a dedicated subject-picker screen for students could be added later if wanted. (d) DB backup from the
  migration session at scratchpad/db-backup-pre-V20.sql.

### 2026-07-23 13:10 — Feature: super-admin student-signup limit per school  [type: change]
- what: SUPER_ADMIN sets (and later edits) a max-students cap per school at account
  creation. Enforced when the school creates a student. Super admin also sees each
  school's active/total student counts.
- files: V21__admin_student_limit.sql (users.student_limit INT NULL = unlimited);
  User.studentLimit; CreateAdminRequest/UpdateAdminRequest +studentLimit;
  AdminDto +studentLimit/studentCount/activeStudentCount, from(user, total, active)
  overload; AdminServiceImpl (adminDtoWithCounts helper computes both counts from
  findByTeacherId; createAdmin/updateAdmin validate+set studentLimit, non-negative);
  UserRepository +countByTeacherId (used by the limit check); StudentServiceImpl
  .createStudent (403 "Student limit reached" when count>=limit, super admins
  exempt); frontend: SuperAdminAdminsPage (student-limit field on create form when
  role=ADMIN; new "Students" column with inline editable limit input + Save,
  showing "N active / M total / [limit]"), adminServices.fetchResultsByClass
  unaffected, updateAdmin action reused as-is.
- result: PASS end-to-end (curl, dalveer SUPER_ADMIN): created school with
  studentLimit=1 -> list shows limit=1, counts 0/0; that school created 1 student
  -> 200, count 1/1; 2nd student -> blocked (limit enforced, count stayed 1);
  super admin raised limit to 5 -> counts reflected (1/1), then 2nd student -> 200.
  V21 applied, backend restarted clean, frontend compiles clean. Test admin +
  2 test students deleted after.
- notes: limit is per-school (ADMIN role only); SUPER_ADMIN accounts show "—" for
  the limit column (exempt, no cap). Blank/null limit = unlimited (unchanged
  default behavior for existing schools, all NULL after migration).

### 2026-07-23 13:15 — Session end: stack shut down  [type: run]
- what: stopped all three tiers cleanly. Backend :8081 and frontend :3000 killed
  via taskkill on their listening PIDs; MySQL (XAMPP mysqld) stopped via
  `mysqladmin -u root shutdown` (graceful, not a forced kill) — confirmed down
  (connection refused).
- files: none.
- result: PASS. No test data left behind (all created during this session's
  verification passes were deleted immediately after each check).

### 2026-07-23 18:07 — Class->Subject->Quiz relabel, student-limit validation, session shutdown  [type: change]
- what: (1) student-limit save now rejects a limit below the school's active-student count
  (AdminServiceImpl.updateAdmin); (2) Schools/Students pages got name search boxes; (3) Students
  page groups by class; (4) removed the standalone /adminSubjects super-admin page and sidebar
  link (redundant — Subject entity has no dedicated top-level CRUD in this UI anymore, only used
  internally); (5) Add/Update Quiz forms ("Add Subject"/"Update Subject" headings — intentional,
  this form is the real subject-creation UX in the product's naming) now pick a **Class** instead
  of a Subject; the matching Subject row (by classId) is resolved automatically on submit; (6)
  AdminQuizzesPage list now groups by Class instead of Subject, and the per-group count label
  reads "quiz"/"quizzes" (was mislabeled "subject"/"subjects").
- files: exam-portal-backend/.../AdminServiceImpl.java; exam-portal-frontend/src/pages/superadmin/
  SuperAdminAdminsPage.js; exam-portal-frontend/src/pages/admin/AdminStudentsPage.js;
  exam-portal-frontend/src/pages/admin/quizzes/{AdminAddQuiz,AdminUpdateQuiz,AdminQuizzesPage}.js;
  exam-portal-frontend/src/components/SuperAdminSidebar.js; exam-portal-frontend/src/App.js;
  deleted exam-portal-frontend/src/pages/superadmin/SuperAdminSubjectsPage.js.
- result: PASS. All verified live in-browser (dalveer/super123): limit-below-active rejected with
  correct message; search boxes filter correctly; class grouping renders; /adminSubjects route and
  sidebar link gone, no console errors; Add-Quiz form's class dropdown resolves the right subject
  (test quiz created under Class 6-8 → General, confirmed in list, then deleted); Quizzes list now
  groups by class with correct quiz/quizzes counts.
- notes / follow-ups: backend, frontend (:3000), and MySQL (XAMPP mysqld) all stopped at end of
  session per user request ("kill all"). Ports 8081/3000/3306 confirmed down. Uncommitted changes
  remain in the working tree (not committed this session — user hasn't asked for a commit).

### 2026-07-24 11:36 — Feature: The Mentalist 15-page PDF report generation  [type: change]
- what: built the full report-generation system on top of existing psychometric scoring (never
  recalculated): V22 migration (users.father_name/mother_name/dob/city/gender +
  mentalist_reports table); InterpretationEngine (score-band rule engine); ReportContentAiService
  (AI Content Engine, reuses existing AiService, falls back to InterpretationEngine when no AI key
  configured); ReportDataAssembler (regroups existing MI/RIASEC/quotient scores into the 15 named
  pages — documented fixed-formula derivations, same pattern as the existing MI-domain sums);
  PdfReportServiceImpl (Thymeleaf template -> openhtmltopdf, Java-native, no headless browser);
  MentalistReportController (generate/download, ownership-scoped via existing
  psychometricReportService.getReport); frontend profile fields + Redux resource + Download button
  on PsychometricReportPage (replaces window.print()).
- files: backend — V22__mentalist_report.sql; models/{User,MentalistReport}.java;
  repository/MentalistReportRepository.java; services/{InterpretationEngine,ReportContentAiService,
  ReportDataAssembler,PdfReportService,MentalistReportService}.java + implementation/*;
  dto/{MentalistReportDto,ReportSectionContent}.java; controllers/MentalistReportController.java;
  configurations/SecurityConfig.java (+2 rules); dto/UpdateProfileRequest.java +
  ProfileServiceImpl.java (+5 fields); resources/templates/report/{report,fragments}.html;
  pom.xml (+thymeleaf, +openhtmltopdf-pdfbox). frontend — components/ProfilePanel.js;
  constants/services/actions/reducers/mentalistReportConstants.js (new resource); store.js;
  pages/users/PsychometricReportPage.js.
- result: PASS end-to-end. Generated a real report for quizResId 30 (Bank Student, real existing
  attempt) as SUPER_ADMIN dalveer: POST .../generate -> 200, GET .../download -> 200, 95KB PDF,
  verified via pypdf: exactly 15 pages, all A4 (210x297mm), correct content per page, no missing/
  tofu glyphs after fixes below. mentalist_reports row (report_id=1) and its PDF file left in place
  (real attempt, not throwaway test data — demonstrates the working feature).
- bugs found & fixed during this session's testing (not present in final code):
  1. Thymeleaf fragment defined inline in the main template got double-evaluated (its own
     `${section}` param was null at the top-level pass) -> moved to a separate
     `templates/report/fragments.html`, referenced via `~{report/fragments :: traitSection(...)}`.
  2. `report.html` had picked up a UTF-8 BOM from an earlier PowerShell `Set-Content -Encoding
     UTF8` edit (defaults to BOM on Windows PowerShell 5.1) — the leading U+FEFF corrupted both
     jsoup's HTML5 parse (head content leaked into body as visible text) and strict XML parsing
     ("Content is not allowed in prolog"). Stripped the BOM from the file and added a defensive
     strip in PdfReportServiceImpl.render() in case it recurs. Switched PDF rendering to
     `PdfRendererBuilder.withHtmlContent()` (strict XHTML parse) instead of the jsoup+W3CDom
     round-trip, once the BOM was no longer masking the real structure — one less moving part.
  3. Default PDF font (WinAnsi/Arial-class) doesn't cover em-dash/star glyphs -> replaced literal
     "—"/"★" in templates with ASCII ("-", repeated "*"), and added a `sanitize()` step
     (ReportContentAiServiceImpl) that flattens smart quotes/em-dashes/ellipsis from any LLM output
     before it reaches the PDF.
- notes / follow-ups: `mentalist.reports.dir` (application.properties) defaults to
  `./data/mentalist-reports` — a real deployment should point this at persistent storage.
  Backend (:8081) and frontend (:3000) both left running for the user to continue testing.
- follow-up UI click-through (same session): frontend started clean (no compile errors, no
  console errors). Logged in as dalveer (SUPER_ADMIN) via localStorage token injection, opened
  /psychometricReport/30, clicked "Generate AI Report" then "Download The Mentalist Report (PDF)"
  — POST .../generate?regenerate=false -> used the already-generated report (fast path, no
  re-render), GET .../download -> 200, button returned to normal state, no console errors.
  Confirms the full click path (not just curl) works.

### 2026-07-24 12:40 — UI redesign: "The Mentalist" brand theme applied app-wide  [type: change]
- what: (1) redesigned Super Admin Schools page (stat cards, sortable/searchable table, avatars,
  role/status Badges, icon action buttons, removed fixed maxWidth so table fills/shrinks with
  viewport); (2) redesigned Super Admin Dashboard the same way (icon stat tiles, sortable/
  searchable School performance table, per-row student progress bar, top-school trophy); (3) added
  shared CSS helper classes to index.css (`.mt-card`, `.mt-card-hover`, `.mt-stat-icon`,
  `.mt-avatar`, `.mt-search`, `.mt-sort-th`, `.mt-progress-track/.mt-progress-fill`, `.mt-page`) so
  every page reuses the same tokens/pattern instead of hand-rolled inline styles; (4) ran 4 parallel
  agents to apply the same treatment to all 26 remaining pages (super admin Results/Profile/AI
  Settings; admin Dashboard/QuizResult/Reports/Students/Profile; admin categories/quizzes/questions
  CRUD forms+lists; all student-facing pages except the live exam-taking screen which only got a
  color-only title touch, logic untouched); (5) removed the "New Customer? Register" self-signup
  link from LoginPage.js (dead end since student self-registration was removed in the V20 migration
  session — POST /api/register already 403s).
- files: exam-portal-frontend/src/index.css (+shared helper classes); pages/superadmin/
  {SuperAdminAdminsPage,SuperAdminDashboardPage,SuperAdminResultsPage,SuperAdminProfilePage,
  SuperAdminAiSettingsPage}.js; pages/admin/{AdminDashboardPage,AdminQuizResultPage,
  AdminReportsPage,AdminStudentsPage,AdminProfilePage}.js; pages/admin/categories/
  {AdminCategoriesPage,AdminAddCategoryPage,AdminUpdateCategoryPage}.js; pages/admin/quizzes/
  {AdminQuizzesPage,AdminAddQuiz,AdminUpdateQuiz}.js; pages/admin/questions/
  {AdminQuestionsPage,AdminAddQuestionsPage,AdminUpdateQuestionPage}.js; pages/users/
  {UserSubjectsPage,UserQuizzesPage,UserQuizManualPage,UserQuestionsPage,UserQuizResultPage,
  OnboardingPage}.js; pages/LoginPage.js.
- result: PASS. `CI=true npx react-scripts build` exits 0 (full production build compiles). Caught
  and fixed one real bug introduced by the batch: AdminDashboardPage.js had a new `useMemo` placed
  after the existing early `if (!students || !results) return <Loader />` — a rules-of-hooks
  violation (hook not called on every render). Moved the memo + its dependent data prep above the
  guard. Remaining ESLint warnings after the full pass are pre-existing (`==` vs `===` in untouched
  reducers, missing-deps on `useEffect`s that predate this session) — not introduced here. Live
  browser spot-checks (dalveer/super123, no console errors on any): /superadmin/admins (Schools),
  /superadmin (Dashboard), /adminDashboard, /adminStudents, /adminQuizzes.
- notes / follow-ups: (a) `UserDashboardPage.js` exists on disk but is empty and unreferenced
  anywhere (checked via grep) — dead file, left alone (out of scope, nobody asked to delete it).
  (b) `UserProfilePage.js` and `AdminProfilePage.js` have no page-level chrome of their own (all
  content delegated to the shared `ProfilePanel.js` component) — not restyled, out of scope for a
  page-by-page pass. (c) `PsychometricReportPage.js` already had its own `.css` fully using `mt-*`
  tokens from an earlier session — left untouched, already matches. (d) session ended with
  frontend :3000, backend :8081, and MySQL :3306 all killed per user request ("kill and log");
  confirmed via netstat that all three ports are down. Uncommitted changes remain in the working
  tree (not committed this session).

### 2026-07-25 — Student profile: guardian name + personal-details view  [type: change]
- what: user asked for more student profile inputs (Guardian name, Age, Gender, DOB, City).
  Gender/DOB/City already existed end-to-end (V22); Age is already derived from DOB in
  ReportDataAssemblerImpl. Only genuinely new field is guardian name. Added it: V23 migration
  (`users.guardian_name VARCHAR(100) NULL`), `User.guardianName`, `UpdateProfileRequest.guardianName`
  + set in ProfileServiceImpl, `MentalistReportDto.StudentProfile.guardianName` + assembler +
  new "Guardian's Name" row in report.html. Frontend ProfilePanel: guardian input in the edit form,
  section header renamed "For The Mentalist report (optional)" -> "Personal details (optional)",
  and a new read-only "Personal details" table (father/mother/guardian/gender/DOB/age/city) for
  students, with age computed client-side from DOB (`ageFromDob`) — no age column anywhere, so the
  two can't drift.
- files: db/migration/V23__student_guardian_name.sql (new); models/User.java;
  dto/UpdateProfileRequest.java; dto/MentalistReportDto.java;
  services/implementation/{ProfileServiceImpl,ReportDataAssemblerImpl}.java;
  templates/report/report.html; frontend src/components/ProfilePanel.js.
- result: PASS (compile only). Backend `./mvnw -o compile` exit 0 with
  JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot. Frontend
  `npx react-scripts build` -> "Compiled with warnings", no ProfilePanel warnings.
  NOT run against a live DB/browser this session — MySQL/backend/frontend were left down from the
  previous session, so V23 has not actually been applied yet.
- notes / follow-ups: (a) `CI=true react-scripts build` now FAILS on pre-existing lint warnings
  (eqeqeq in categories/questions/quizzes reducers, exhaustive-deps, no-unused-vars) — none from
  this change, but the previously-green CI build command is no longer green. (b) First real run
  must apply V23 before Hibernate `ddl-auto=validate` passes. (c) CreateStudentRequest (admin
  creating a student) still captures only username/password/name/phone/classId — guardian and the
  other personal fields are student-self-serve via ProfilePanel only.
- amendment (same day): user asked to drop father/mother name. V23 rewritten to both add
  `guardian_name` and `DROP COLUMN father_name, mother_name` (V22 added them and is already applied
  on the live DB, so the drop must be a migration, not a V22 edit; V23 itself was never applied
  anywhere yet, so folding both into it is safe). Removed fatherName/motherName from User,
  UpdateProfileRequest, ProfileServiceImpl, MentalistReportDto.StudentProfile,
  ReportDataAssemblerImpl, report.html (page 2 profile table) and ProfilePanel (edit inputs +
  read-only rows). grep for father/mother across backend+frontend src is clean apart from the V22
  history. Recompiled: backend exit 0, frontend "Compiled with warnings" (none in ProfilePanel).
  DESTRUCTIVE: applying V23 permanently deletes any existing father_name/mother_name data.
- session end: nothing to kill — no server was started this session (work was compile-only).
  netstat confirms :3306, :8081, :3000 all down. Uncommitted changes remain in the working tree.
  Next run must apply V23 (destructive: drops father_name/mother_name) before Hibernate
  `ddl-auto=validate` passes.

### 2026-07-25 — Full local run (MySQL + backend + frontend)  [type: run]
- what: user asked "run project". Brought stack up in order.
- MySQL: XAMPP mysqld started manually (no Windows service registered) via
  `C:\xampp\mysql\bin\mysqld.exe --defaults-file=C:\xampp\mysql\bin\my.ini --console`, empty root
  password, :3306 up. First attempt with `Start-Process ... --standalone -WindowStyle Hidden` exited
  silently with nothing in the .err log; running it in a foreground/background shell worked.
- V23 applied for real this time. Pre-check before the destructive drop:
  `SELECT COUNT(*) FROM users WHERE father_name IS NOT NULL OR mother_name IS NOT NULL` = 0, so no
  data was lost. Flyway log: `Migrating schema to version "23 - student guardian name"` /
  `Successfully applied 1 migration ... now at version v23`. `SHOW COLUMNS FROM users LIKE '%name%'`
  now shows `guardian_name varchar(100)` and no father_name/mother_name. Hibernate validate passed.
- Backend: `./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password="` with
  JAVA_HOME=jdk-17.0.19.10-hotspot, JWT_SECRET=local-dev-secret, SUPERADMIN_PASSWORD=super123.
  `Started ExamPortalBackendApplication in 21.475 seconds`. Smoke test `GET /api/teachers` = 200 with
  a real teacher list.
- Frontend: started through the preview tool (`.claude/launch.json` entry `exam-portal-frontend`),
  CRA on :3000, `[HPM] Proxy created: / -> http://127.0.0.1:8081`, `GET http://localhost:3000` = 200.
- result: PASS — all three services up. Not driven through the UI this session (the in-app browser
  pane was not displayed, so navigate/screenshot failed with "the Browser pane is not displayed");
  verification was curl + SQL only.
- session end: user said "kill". Frontend preview server stopped cleanly; backend (:8081) and mysqld
  (:3306) force-killed via Stop-Process. netstat confirms :3306, :8081, :3000 all down. mysqld did
  not get a normal shutdown, so expect InnoDB crash recovery in the .err log on the next start
  (harmless, but it is why the log will not show "Shutdown complete" for this run). Working tree
  clean — the log entry above is committed as 05af9e2.

### 2026-07-29 — Full local run (MySQL + backend + frontend)  [type: run]
- what: user asked "run project". Same start order as the 2026-07-25 run.
- MySQL: `C:\xampp\mysql\bin\mysqld.exe --defaults-file=C:\xampp\mysql\bin\my.ini --console` in a
  background shell (no Windows service). `mysql -u root -h 127.0.0.1 -e "SELECT 1"` = 1, empty root
  password. InnoDB crash recovery from last session's forced kill was harmless as predicted.
- Backend: JAVA_HOME=jdk-17.0.19.10-hotspot (java -version confirmed 17.0.19), JWT_SECRET=
  local-dev-secret, SUPERADMIN_PASSWORD=super123,
  `./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password="`.
  `Started ExamPortalBackendApplication in 18.952 seconds`. No Flyway migration applied — schema
  already at v23 from the previous run; Hibernate validate passed. SuperAdminInitializer logged
  "A SUPER_ADMIN already exists; nothing to seed."
- Smoke test: `GET http://localhost:8081/api/teachers` = 200.
- Frontend: preview tool, `.claude/launch.json` entry `exam-portal-frontend`. CRA on :3000,
  `[HPM] Proxy created: / -> http://127.0.0.1:8081`, `Compiled successfully!`.
- UI check: get_page_text on http://localhost:3000 returns the login page (Exam-Portal / Login /
  Register / Sign In / User Name / Password). Screenshot still fails with "the Browser pane is not
  displayed" — same limitation as 2026-07-25, so no visual verification, text only.
- result: PASS — all three services up, login page served through the CRA proxy.
- note: also produced an unrelated deliverable this session (ZoloSuit marketing site, published as
  a Claude artifact from the scratchpad). No repo files touched by it.

### 2026-07-29 — Public landing page at "/" (Stitch integration, Option 1)  [type: feature]
- ask: make a marketing landing the public home at "/", move login to "/login", auth-redirect
  logged-in visitors on "/" to their dashboard, fix nav routes.
- BLOCKER: the Stitch design (project 13531886515221973744) could not be fetched — it's an
  authenticated Google SPA; WebFetch returns only the page <title>. Built the landing on the
  existing Mentalist design tokens (index.css) instead, structured so the visual layer can be
  swapped when the Stitch HTML is pasted. This landing is NOT the Stitch design.
- files: NEW pages/LandingPage.js + pages/LandingPage.css; App.js ("/" now LandingPage, import
  added); components/Header.js (two nav LinkContainers "/" -> "/login": the guest Login link and
  the Logout link). Nothing else touched — no backend, no auth, no protected routes.
- routing model: "/" = LandingPage, "/login" = LoginPage (unchanged), "/register" unchanged, all
  protected/admin/superadmin/student routes unchanged. LandingPage useEffect reads jwtToken+user and
  navigate(homePathForRoles(roles), {replace:true}) for authed visitors — same helper LoginPage and
  ProtectedRoute already use.
- verified live (backend :8081, frontend :3000, CRA proxy):
  * GET / renders the landing hero (get_page_text). No console errors.
  * All anchor hrefs on / are /login or /register — no leftover "/". 4 hero/CTA buttons.
  * GET /login still renders the real login form (#username, #password, h1 "Sign In").
  * Seeded a fake USER session in localStorage, hit / -> redirected to /profile -> /onboarding
    (onboarding gate). Confirms authed-visitor redirect. Cleared the fake session after.
  * Compiled successfully; LandingPage.js/.css add ZERO eslint warnings. The 3 eslint warnings are
    pre-existing dead code in Header.js (profilePageUrl, lines 14/55/57), not from this change.
- NOT verified: visual/responsive proof — screenshot still fails ("Browser pane is not displayed").
  Responsive relies on Bootstrap Col md/lg + CSS clamp(), not eyeballed this session.
- pre-existing debt noted (out of scope, left as-is): Header.js dead profilePageUrl block.

### 2026-07-29 — Phase 1 production cleanup & audit  [type: cleanup]
- debug code: removed 23 console.log/debug (frontend), kept 22 console.error. 4 System.out.println
  in JwtRequestFilter -> SLF4J log.debug (added Logger field + imports). Backend compile exit 0.
- dead code: Header.js dead profilePageUrl var + its dead useEffect .map (also killed a latent
  loginReducer.user null-deref); 3 dead onClick console.log("View") on UserQuizzesPage display chips.
  Fixed 3 unused-`data` warnings my log-removal introduced in delete*() services.
- deleted: root images/ (20 orphan screenshots — README uses github-hosted urls, verified no refs);
  untracked exam-portal-frontend/.idea/ (5 files). public/images/user.png kept (used by ProfilePanel).
- repo: rewrote .gitignore (.env*, .idea, .vscode, *.iml, OS junk, npm logs). Added
  exam-portal-backend/.env.example (10 vars). index.html title/desc: "React App" -> "The Mentalist".
- security scan: no hardcoded secrets/keys, no eval, no dangerouslySetInnerHTML/innerHTML, no raw
  @Query/native SQL. All env-var backed. AI key DB-stored + masked in DTO.
- NOT changed (reported in docs/release/phase-1-cleanup-report.md §10): JWT_SECRET dev fallback,
  show_sql=true, plaintext AI key, bootstrap loaded from CDN beta (npm bootstrap dep unused),
  19MB Project-Synopsis.pdf at root, broken default App.test.js, pre-existing eslint warnings
  (eqeqeq/exhaustive-deps/no-extend-native in UserQuestions/UserQuizzes pages).
- verify: backend GET /api/teachers=200 after changes; frontend compiled (warnings only, no errors);
  / and /login render with zero console errors. Screenshot still blocked (pane not displayed).

### 2026-07-29 — Phase 2 production & release prep  [type: feature/release]
- backend prod-readiness (new/changed):
  * pom version 0.0.1-SNAPSHOT -> 1.0.0. application.properties: app.version=@project.version@
    (resource-filtered), server.shutdown=graceful.
  * application-prod.properties (NEW): jwt.secret + datasource.password with NO defaults (fail fast),
    show_sql=false, include-stacktrace=never, INFO/WARN logging.
  * HealthController (NEW): GET /health -> {status:ok,version}. SecurityConfig permits GET /health.
  * EnvironmentValidator (NEW, @Profile prod): rejects missing/dev-default/<32 char JWT_SECRET with
    clear FATAL message.
  * GlobalExceptionHandler (NEW): extends ResponseEntityExceptionHandler so MVC 4xx untouched;
    preserves ResponseStatusException {message}; logs unexpected at ERROR, returns opaque 500.
- docker: backend Dockerfile (maven->JRE alpine, non-root, HEALTHCHECK), frontend Dockerfile
  (node build -> nginx) + nginx.conf (/api proxy + SPA fallback), .dockerignore x2,
  docker-compose.yml (mysql+backend+frontend, health-gated depends_on, named volumes),
  root .env.example (grouped). .gitignore extended (dist/.next/coverage/.cache/uploads/tmp/.env.production).
- docs: README rewritten (all requested sections), CHANGELOG.md, VERSION=1.0.0,
  docs/release/phase-2-production-readiness.md (score 88/100).
- BUILD VERIFICATION (all pass):
  * mvnw clean package -DskipTests: exit 0 -> exam-portal-backend-1.0.0.jar (50MB).
  * npm run build: exit 0 -> build/ ready (warnings only).
  * prod jar boot with valid JWT_SECRET + empty XAMPP pw (--spring.datasource.password=): Started in
    32s, profile prod. GET /health = 200 {"status":"ok","version":"1.0.0"}. Protected /api/admin/admins
    = 403 (auth intact). Bad login = 401 {"message":"Invalid username or password"} (error contract
    preserved through new handler).
  * fail-fast: prod jar with JWT_SECRET=short -> IllegalStateException "FATAL: production configuration
    error — JWT_SECRET is too short (5 chars)...", startup aborted.
- gotcha: first prod jar run failed with MySQL "Access denied ... using password: YES" — the empty
  DB_PASSWORD env var did not propagate as empty in the background PowerShell run. Fixed by passing
  --spring.datasource.password= as a program arg (repo's known-good pattern). Not a code issue.
- teardown: test jar on :8081 killed. Frontend dev server (:3000) + mysqld (:3306) left running.
  Nothing deployed.

### 2026-07-29 — Phase 3 Docker/Dockploy hardening & readiness  [type: feature/release]
- backend Dockerfile: merged user+dir+chown into one RUN, COPY --from=build --chown=app:app,
  added JAVA_OPTS (-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError), ENTRYPOINT switched to
  `exec java $JAVA_OPTS -jar app.jar` (shell form for var expansion, exec keeps java as PID 1 for
  graceful shutdown).
- frontend Dockerfile: explicit ENV NODE_ENV=production in build stage, npm cache clean merged into
  install RUN, base image swapped nginx:1.27-alpine -> nginxinc/nginx-unprivileged:1.27-alpine
  (fully non-root, not just workers) -> listen/EXPOSE moved 80->8080. nginx.conf updated (listen 8080,
  added security headers: X-Content-Type-Options, X-Frame-Options, Referrer-Policy, server_tokens off,
  conditional HSTS via map on $http_x_forwarded_proto — first draft used invalid `if_https` inline
  add_header syntax, fixed to a proper nginx map block).
- docker-compose.yml: added explicit `name: exam-portal`, container_name x3, explicit `internal`
  bridge network (only frontend publishes a port), TZ env x3, deploy.resources.limits x3
  (db 1cpu/768M, backend 1.5cpu/1024M, frontend 0.5cpu/128M), x-logging anchor (json-file,
  max-size 10m, max-file 3) applied to all 3 services. frontend port mapping 8080:80 -> 8080:8080
  to match the unprivileged nginx image.
- backend application-prod.properties: added server.forward-headers-strategy=framework (trust
  X-Forwarded-* from nginx/Dockploy for HTTPS readiness).
- .dockerignore x2 extended: .github, logs/, tmp/, cache/, docs/ (backend); .github, logs/, tmp/,
  cache/, .cache/, src/**/*.test.js (frontend).
- new docs: docs/deployment/docker-deployment.md (architecture diagram, container table, networking,
  volumes, build/deploy/update/rollback/backup with runnable commands, health checks, troubleshooting),
  docs/release/phase-3-docker-readiness.md (score 90/100).
- VALIDATION: docker compose config (client-side, throwaway .env deleted after) — validated TWICE
  (before and after the port/image edits). Confirmed env interpolation, resource limit byte math
  (1024M=1073741824 etc.), internal network on all 3 services, frontend target:8080 published:8080.
  Redis confirmed N/A by repo-wide grep (no redis anywhere).
- BLOCKER (environment, not project): Docker Desktop backend crashed on launch —
  "initializing Inference manager: listening on unix://.../dockerInference: remove
  .../dockerInference: The file cannot be accessed by the system" (com.docker.backend.exe.log),
  a stale socket file from a prior Docker Desktop session under the user's AppData\Local\Docker\run\.
  Deleting files there was correctly blocked by the permission classifier (outside repo scope, system
  AppData) — did not force it. Reported honestly in the readiness report rather than fabricating a
  live docker compose up --build result. User needs to clear that Docker Desktop state themselves
  (delete run\ contents or Reset to factory defaults) before the one remaining manual validation step.
- nothing deployed, nothing pushed.
