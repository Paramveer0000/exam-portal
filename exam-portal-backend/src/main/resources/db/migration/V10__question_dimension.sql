-- Every question measures exactly one psychometric dimension:
--   MI (9): LOGICAL, MUSICAL, NATURALIST, VERBAL, INTERPERSONAL, KINESTHETIC,
--           SPATIAL, INTRAPERSONAL, EXISTENTIAL
--   RIASEC (6): R, I, A, S, E, C
-- The platform is psychometric-only, so the column is NOT NULL. Legacy rows
-- (created before dimensions existed) are backfilled to LOGICAL.
ALTER TABLE questions
    ADD COLUMN dimension VARCHAR(20) NULL AFTER answer;

UPDATE questions SET dimension = 'LOGICAL' WHERE dimension IS NULL;

ALTER TABLE questions
    MODIFY COLUMN dimension VARCHAR(20) NOT NULL;
