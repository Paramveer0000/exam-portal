# Psychometric Question Banks — Construct Sources

The sample item banks in `seed/psychometric/` are **original items** written to reflect the
public, non-proprietary constructs below. No items were copied from any commercial or
copyrighted instrument (MIDAS, Self-Directed Search, "Brain Wizard", etc.). Every item is a
plain first-person self-report statement answered on a 4-point word-anchored agreement scale.

## Frameworks

### Gardner's Multiple Intelligences (9)
Nine relatively independent ways of processing information. The behaviours each dimension
reflects, used to write items:

| Code | Intelligence | Behaviours the items probe |
|------|--------------|----------------------------|
| LOGICAL | Logical-Mathematical | reasoning, numbers, patterns, problem-solving, cause/effect |
| MUSICAL | Musical-Rhythmic | rhythm, pitch, melody, remembering/making music |
| NATURALIST | Naturalistic | recognising and classifying plants, animals, natural phenomena |
| VERBAL | Verbal-Linguistic | words, reading, writing, storytelling, learning languages |
| INTERPERSONAL | Interpersonal | reading others' moods, cooperating, leading, helping |
| KINESTHETIC | Bodily-Kinesthetic | body control, sport, building/making with hands |
| SPATIAL | Visual-Spatial | visualising, maps, drawing, 3-D/mental rotation |
| INTRAPERSONAL | Intrapersonal | self-awareness, reflecting on own feelings/goals |
| EXISTENTIAL | Existential | big "why" questions, meaning, purpose, life and the universe |

### Holland RIASEC (6)
Six work-personality / interest types. Item activities per type:

| Code | Type | Typical interests/activities |
|------|------|------------------------------|
| R | Realistic (Doers) | tools, machines, building, outdoors, hands-on/technical |
| I | Investigative (Thinkers) | investigating, experiments, research, understanding how things work |
| A | Artistic (Creators) | creating, art, design, music, writing, self-expression |
| S | Social (Helpers) | helping, teaching, caring, working with people |
| E | Enterprising (Persuaders) | leading, persuading, selling, organising ventures |
| C | Conventional (Organizers) | organising, records, data, order, following clear procedures |

### Age-appropriate self-report items
- 4 word-anchored response options ("Strongly disagree → Strongly agree") — the literature
  finds ~4 word-based options optimal for youth self-report; matches the app's 4-option model.
- Concrete wording, short single-clause sentences, no double-barrelled items, vocabulary
  scaled per grade band (6–8 simplest, 11–12 most abstract).

## Sources (public / non-proprietary)
- Gardner's Multiple Intelligences — Simply Psychology: https://www.simplypsychology.org/multiple-intelligences.html
- Multiple Intelligences overview — NIU Center for Innovative Teaching & Learning: https://www.niu.edu/citl/resources/guides/instructional-guide/gardners-theory-of-multiple-intelligences.shtml
- Theory of Multiple Intelligences — Wikipedia: https://en.wikipedia.org/wiki/Theory_of_multiple_intelligences
- RIASEC theory — Self-Directed Search (theory page): https://self-directed-search.com/riasec-theory/
- Holland Codes — Wikipedia: https://en.wikipedia.org/wiki/Holland_Codes
- Holland's Six Personality Types — Career Key: https://www.careerkey.org/fit/personality/holland-personality-types
- Likert scales with children (4 word-anchored options optimal) — ResearchGate summary: https://www.researchgate.net/publication/258103869_The_Use_of_Likert_Scales_With_Children
- Readability of youth self-report measures — MDPI IJERPH 2022: https://www.mdpi.com/1660-4601/19/15/9555

## Scoring format the items target
- Answer scale stored per item as four options; the app scores the **ordinal of the chosen
  option** (option1=1 … option4=4). Items are phrased so option4 = "Strongly agree" is the
  high-trait response. The `answer` field (a required "correct option" in the schema) is set
  to `option4` by convention and is **not** used by psychometric scoring.
