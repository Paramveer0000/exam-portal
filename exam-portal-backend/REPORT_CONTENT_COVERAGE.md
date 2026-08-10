# Report Content Coverage

Generated for the Report Content Engine V2 sprint. Verified by
`DimensionContentServiceImplTest`, which fails the build if any seeded dimension
loses its authored content.

## Where content lives

| Concern | Location |
|---|---|
| Dimension prose | `src/main/resources/content/dimensions-*.json` |
| Band paragraphs, section intros, career clusters, streams, synthesis themes, parent/teacher guides | `src/main/resources/content/report-content.json` |
| Loader | `services/implementation/DimensionContentServiceImpl.java` |
| Scores | unchanged — `dimension_results` / `psychometric_reports` |

Content files are read at startup. Adding a `dimensions-<category>.json` file
requires no code change. No dimension prose lives in Java, Thymeleaf or CSS.

## Totals

| Metric | Count |
|---|---|
| Dimensions seeded in DB (V26 + V28) | 48 |
| Dimensions with a content profile | 48 |
| Dimensions with **complete** content (all 9 fields) | 48 |
| Dimensions using fallback/filler content | 0 |
| Interpretation bands with authored copy | 5 / 5 |
| Dimension types with a section intro | 6 / 6 |
| Career clusters with content | 12 / 12 |
| Streams with content | 4 / 4 |
| Cross-dimension synthesis themes | 8 |

Every content profile carries: `whatItMeasures`, `looksLike`, `learningImplications`,
`strengths`, `development`, `activities`, `careerConnections`, `parentTip`, `studentTip`.

## Coverage by dimension

Legend: **D** description/whatItMeasures · **L** learning implications ·
**A** activities · **V** development opportunities · **C** career connections ·
**P** parent guidance. All rows are complete, so all cells are `Y`.

### Multiple Intelligences (MI) — 9/9

| Code | Name | Type | D | L | A | V | C | P | Status |
|---|---|---|---|---|---|---|---|---|---|
| LOGICAL | Logical-Mathematical | MI | Y | Y | Y | Y | Y | Y | COMPLETE |
| VERBAL | Verbal-Linguistic | MI | Y | Y | Y | Y | Y | Y | COMPLETE |
| SPATIAL | Visual-Spatial | MI | Y | Y | Y | Y | Y | Y | COMPLETE |
| MUSICAL | Musical-Rhythmic | MI | Y | Y | Y | Y | Y | Y | COMPLETE |
| KINESTHETIC | Bodily-Kinesthetic | MI | Y | Y | Y | Y | Y | Y | COMPLETE |
| INTERPERSONAL | Interpersonal | MI | Y | Y | Y | Y | Y | Y | COMPLETE |
| INTRAPERSONAL | Intrapersonal | MI | Y | Y | Y | Y | Y | Y | COMPLETE |
| NATURALIST | Naturalistic | MI | Y | Y | Y | Y | Y | Y | COMPLETE |
| EXISTENTIAL | Existential | MI | Y | Y | Y | Y | Y | Y | COMPLETE |

### RIASEC — 6/6

| Code | Name | Type | D | L | A | V | C | P | Status |
|---|---|---|---|---|---|---|---|---|---|
| R | Realistic | RIASEC | Y | Y | Y | Y | Y | Y | COMPLETE |
| I | Investigative | RIASEC | Y | Y | Y | Y | Y | Y | COMPLETE |
| A | Artistic | RIASEC | Y | Y | Y | Y | Y | Y | COMPLETE |
| S | Social | RIASEC | Y | Y | Y | Y | Y | Y | COMPLETE |
| E | Enterprising | RIASEC | Y | Y | Y | Y | Y | Y | COMPLETE |
| C | Conventional | RIASEC | Y | Y | Y | Y | Y | Y | COMPLETE |

### Emotional Intelligence (EQ) — 11/11

| Code | Name | Type | D | L | A | V | C | P | Status |
|---|---|---|---|---|---|---|---|---|---|
| EMPATHY | Empathy / Social Awareness | EQ | Y | Y | Y | Y | Y | Y | COMPLETE |
| STRESS_MGMT | Stress Management | EQ | Y | Y | Y | Y | Y | Y | COMPLETE |
| SELF_MOTIVATION | Self-Motivation | EQ | Y | Y | Y | Y | Y | Y | COMPLETE |
| RESILIENCE | Self-Motivation / Resilience | EQ | Y | Y | Y | Y | Y | Y | COMPLETE |
| RESPONSE_CTRL | Self-Regulation / Response Control | EQ | Y | Y | Y | Y | Y | Y | COMPLETE |
| IMPULSE_CTRL | Impulse Control / Thoughtful Communication | EQ | Y | Y | Y | Y | Y | Y | COMPLETE |
| ANGER_MGMT | Self-Regulation / Anger Management | EQ | Y | Y | Y | Y | Y | Y | COMPLETE |
| CONFLICT_CTRL | Emotional Regulation / Conflict Control | EQ | Y | Y | Y | Y | Y | Y | COMPLETE |
| RELATIONSHIP_MGMT | Accountability / Relationship Management | EQ | Y | Y | Y | Y | Y | Y | COMPLETE |
| ENCOURAGEMENT | Social Skills / Encouraging Others | EQ | Y | Y | Y | Y | Y | Y | COMPLETE |
| FEEDBACK_ACCEPT | Self-Awareness / Feedback Acceptance | EQ | Y | Y | Y | Y | Y | Y | COMPLETE |

> **Note.** `dimensions.description` is NULL for all EQ and LEADERSHIP rows in the
> database (seeded that way by V28). That gap, flagged in the previous sprint, is
> now closed at the content layer: the report draws `whatItMeasures` from the
> JSON profile rather than from the DB column. The DB column itself is still NULL
> and is left untouched — no migration was added.

### Leadership & Soft Skills — 6/6

| Code | Name | Type | D | L | A | V | C | P | Status |
|---|---|---|---|---|---|---|---|---|---|
| COMMUNICATION | Communication | LEADERSHIP | Y | Y | Y | Y | Y | Y | COMPLETE |
| TEAMWORK | Teamwork | LEADERSHIP | Y | Y | Y | Y | Y | Y | COMPLETE |
| OWNERSHIP | Responsibility / Ownership | LEADERSHIP | Y | Y | Y | Y | Y | Y | COMPLETE |
| DECISION_CONF | Decision-Making Confidence | LEADERSHIP | Y | Y | Y | Y | Y | Y | COMPLETE |
| ADAPTABILITY | Adaptability | LEADERSHIP | Y | Y | Y | Y | Y | Y | COMPLETE |
| CONFLICT_RES | Conflict Resolution | LEADERSHIP | Y | Y | Y | Y | Y | Y | COMPLETE |

### Learning Preference — 3/3

| Code | Name | Type | D | L | A | V | C | P | Status |
|---|---|---|---|---|---|---|---|---|---|
| VISUAL_LEARNING | Visual Learning | LEARNING_PREF | Y | Y | Y | Y | Y | Y | COMPLETE |
| AUDITORY_LEARNING | Auditory Learning | LEARNING_PREF | Y | Y | Y | Y | Y | Y | COMPLETE |
| KINESTHETIC_LEARNING | Kinesthetic Learning | LEARNING_PREF | Y | Y | Y | Y | Y | Y | COMPLETE |

### Career Interest — 13/13

| Code | Name | Type | D | L | A | V | C | P | Status |
|---|---|---|---|---|---|---|---|---|---|
| ENGINEERING | Engineering | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |
| TECHNOLOGY | Technology | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |
| MEDICAL | Medical | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |
| PSYCHOLOGY | Psychology | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |
| TEACHING | Teaching | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |
| MANAGEMENT | Management | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |
| ENTREPRENEURSHIP | Entrepreneurship | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |
| COMMERCE | Commerce | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |
| ARTS | Arts | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |
| DESIGN | Design | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |
| GOVERNMENT | Government | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |
| HOSPITALITY | Hospitality | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |
| ENVIRONMENT | Environment | CAREER_INTEREST | Y | Y | Y | Y | Y | Y | COMPLETE |

## Career cluster coverage — 12/12

Keyed by `career_suggestions.field` (seeded by V12). Each carries `whatItIs`,
`exampleRoles` and `subjectsToExplore`. The "why this appears" line is generated
per student from `career_suggestions.dimensions` matched against that student's
own stronger results — it is not authored text.

Engineering & Technology · Data & Research Science · Medicine & Health Care ·
Business & Management · Finance & Accounting · Law & Public Policy ·
Media & Communication · Design & Creative Arts · Performing Arts & Music ·
Sports & Physical Sciences · Education & Social Work · Environment & Agriculture

## Stream coverage — 4/4

Science · Commerce · Humanities · Vocational. Rendered only when
`AssessmentContext.isStreamGuidanceAppropriate()` is true (Class 11+).

## Synthesis themes — 8

ANALYTICAL · VISUAL_CREATIVE · PEOPLE · LEADING · PRACTICAL · STRUCTURED ·
EXPRESSIVE · REFLECTIVE

A theme is reported only when at least 2 of the student's leading dimensions
match it; at most 2 themes are shown. Ties break on theme code, so output is
deterministic.

## Known gaps

1. **`dimensions.description` remains NULL for EQ and LEADERSHIP in the database.**
   The report no longer depends on it, but any other consumer of that column
   still sees NULL. Closing it needs an additive migration, deliberately not
   added in this sprint.
2. **Behaviour and Wellness sections remain legacy.** They are still derived from
   hardcoded MI/RIASEC/quotient proxies in `ReportDataAssemblerImpl`; no
   `dimension_type` exists for them, so they have no content profiles and no
   dimension pages.
3. **Personality section remains legacy** for the same reason.
4. **Content is English only.** No localisation mechanism exists.
5. **Content is file-backed, not admin-editable at runtime.** Editing requires a
   deploy. The loader is structured so a DB-backed source could replace the JSON
   without touching the report layer.
