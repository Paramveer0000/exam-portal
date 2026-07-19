# Psychometric Scoring - Accuracy Validation

Automated fixtures submitted through the real API (band **Class 11-12**, quizId 21) as
student `psychobankstudent`; each row asserts the persisted report from
`GET /api/psychometric-report/{quizResId}`. Regenerate with `validate-scoring.ps1`.

**Result: 18 / 18 passed** (run 2026-07-20 00:06)

Scale: each item answered `Strongly agree` = 4 (spike) or `Disagree` = 2 (baseline).

| # | Fixture | Input | Expected | Actual | Result |
|---|---------|-------|----------|--------|--------|
| 1 | MI spike: LOGICAL ranks #1 | all LOGICAL=agree(4), rest=disagree(2) | LOGICAL rank1 | rank1=LOGICAL (20.0%) | PASS |
| 2 | MI spike: LOGICAL -> IQ top quotient | same fixture | IQ=100 (max) | IQ=100.0 | PASS |
| 3 | MI spike: LOGICAL career leaning | same fixture | top-3 includes one of: Engineering & Technology, Data & Research Science, Finance & Accounting | top3=Finance & Accounting / Data & Research Science / Business & Management | PASS |
| 4 | MI spike: VERBAL ranks #1 | all VERBAL=agree(4), rest=disagree(2) | VERBAL rank1 | rank1=VERBAL (20.0%) | PASS |
| 5 | MI spike: VERBAL -> IQ top quotient | same fixture | IQ=100 (max) | IQ=100.0 | PASS |
| 6 | MI spike: VERBAL career leaning | same fixture | top-3 includes one of: Media & Communication, Law & Public Policy, Education & Social Work | top3=Law & Public Policy / Media & Communication / Engineering & Technology | PASS |
| 7 | MI spike: INTERPERSONAL ranks #1 | all INTERPERSONAL=agree(4), rest=disagree(2) | INTERPERSONAL rank1 | rank1=INTERPERSONAL (20.0%) | PASS |
| 8 | MI spike: INTERPERSONAL -> EQ top quotient | same fixture | EQ=100 (max) | EQ=100.0 | PASS |
| 9 | MI spike: INTERPERSONAL career leaning | same fixture | top-3 includes one of: Business & Management, Education & Social Work, Medicine & Health Care | top3=Business & Management / Education & Social Work / Medicine & Health Care | PASS |
| 10 | MI spike: KINESTHETIC ranks #1 | all KINESTHETIC=agree(4), rest=disagree(2) | KINESTHETIC rank1 | rank1=KINESTHETIC (20.0%) | PASS |
| 11 | MI spike: KINESTHETIC -> AQ top quotient | same fixture | AQ=100 (max) | AQ=100.0 | PASS |
| 12 | MI spike: KINESTHETIC career leaning | same fixture | top-3 includes one of: Sports & Physical Sciences, Performing Arts & Music | top3=Performing Arts & Music / Sports & Physical Sciences / Engineering & Technology | PASS |
| 13 | RIASEC spike: R is dominant | all R=agree(4), rest=disagree(2) | R dominant, Holland starts R | R.dominant=True, Holland=RIA | PASS |
| 14 | RIASEC spike: R hands-on careers on top | same fixture | top-3 includes one of: Engineering & Technology, Sports & Physical Sciences, Environment & Agriculture | top3=Sports & Physical Sciences / Engineering & Technology / Environment & Agriculture | PASS |
| 15 | Sum sanity: MI percents total ~100 | LOGICAL spike | sum in 99-101 | sum=100 | PASS |
| 16 | Sum sanity: Analytical domain = fixed sum | LOGICAL spike | Analytical=40.0 | Analytical=40.0 | PASS |
| 17 | Sum sanity: ranks are a 1..9 permutation | LOGICAL spike | 1..9 each once | ranks=1,2,3,4,5,6,7,8,9 | PASS |
| 18 | Boundary: uniform answers -> even MI + no crash | every item=disagree(2) | spread<2 and ranks valid | spread=0, ranksValid=True | PASS |

## Notes
- MI percent = a dimension's share of total MI Likert points; a single spiked
  dimension (16 pts vs 8 baseline) reads ~20% and ranks #1, as expected.
- Quotients are normalized so the strongest reads 100%. A spike on either MI
  dimension feeding a quotient drives that quotient to the top (IQ=LOGICAL+VERBAL,
  EQ=INTERPERSONAL+INTRAPERSONAL, AQ=NATURALIST+KINESTHETIC).
- Career ranking follows the driving dimensions in `career_suggestions`.

## Finding that led to a formula change
- **Career scale mismatch (fixed).** The first run failed one case: a KINESTHETIC
  (MI) spike still ranked *Engineering* above *Sports/Performing Arts*. Root cause:
  career scoring averaged MI shares (~10-20 each) with RIASEC scores mapped to
  0-100, so at baseline any RIASEC-driven field outweighed an MI spike. Fixed by
  scoring each driving dimension as its **prominence within its own system**
  (value / that system's average; 1.0 = average), putting MI and RIASEC on equal
  footing. After the change all 18 fixtures pass and MI spikes correctly steer
  their careers (KINESTHETIC -> Performing Arts/Sports, LOGICAL -> Finance/Data).

