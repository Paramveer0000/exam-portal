-- Additive only. NULL weight (the default for every existing row) keeps
-- meaning "equal split across this question's mapped dimensions" -- no
-- existing row's semantics change and none are backfilled.
ALTER TABLE question_dimensions ADD COLUMN weight DECIMAL(4,3) NULL;
