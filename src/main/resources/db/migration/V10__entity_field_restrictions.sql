ALTER TABLE concept
    ALTER COLUMN description TYPE VARCHAR(500);

ALTER TABLE mark_scheme
    ALTER COLUMN body TYPE VARCHAR(10000);

ALTER TABLE question
    ALTER COLUMN body TYPE VARCHAR(10000);

ALTER TABLE question_attempt
    ALTER COLUMN body TYPE VARCHAR(25000);

-- A client-generated UUID is 36 characters; 64 leaves room for a different token format without a
-- migration. Bounded because the value is client-supplied and indexed: an oversized token would otherwise
-- fail at insert on Postgres's btree entry limit rather than at validation.
ALTER TABLE question_attempt
    ALTER COLUMN session_token TYPE VARCHAR(64);
