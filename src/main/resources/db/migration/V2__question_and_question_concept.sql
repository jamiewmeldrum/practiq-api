-- Questions served to students once approved. Body is Markdown, with images
-- embedded via {{s3:key}} refs and MCQ choices via - [ ] / - [x] lists (see
-- D-009). No level column — serving context comes from the concept mapping,
-- not the question itself (see D-010). source_spec is a provenance stopgap
-- until a specification FK exists. difficulty and type are nullable because
-- AI extraction may not confidently populate them yet (see D-012). Enum-like
-- values (type, source, status) are stored upper-case to match Java enum
-- constant names exactly, so @Enumerated(EnumType.STRING) works with no
-- custom AttributeConverter (see D-013) — this convention applies to every
-- enum-backed column project-wide. source distinguishes self-authored (SEED),
-- AI-extracted-from-a-real-document (EXTRACTED), and genuinely AI-synthesised
-- (GENERATED) content — a real copyright/compliance distinction (see D-014).
-- A link back to the originating S3 document is deferred to Sprint 0.3,
-- added alongside the document table this needs to reference (see D-014).
-- No mark_scheme column — mark schemes are not a Question concern; where
-- they live instead is an open design question, not yet decided (see D-018).
create table question
(
    id          bigint generated always as identity primary key,
    version     integer not null default 0,
    body        text        not null,
    difficulty  integer check (difficulty between 1 and 5),
    type        varchar(20) check (type in ('SHORT_ANSWER', 'EXTENDED', 'MCQ')),
    source      varchar(20) not null check (source in ('SEED', 'EXTRACTED', 'GENERATED')),
    status      varchar(20) not null default 'PENDING' check (status in ('PENDING', 'APPROVED', 'REJECTED')),
    source_spec varchar(255),
    created_at  timestamptz not null default now()
);

create index idx_question_approved_created
    on question (created_at asc, id asc)
    where status = 'APPROVED';

-- Join table linking questions to the concepts they cover. A question can cover
-- multiple concepts (synoptic questions); a concept can be covered by many questions.
-- ON DELETE CASCADE: a link row has no meaning without both sides existing, so
-- removing either a question or a concept should remove the links that reference
-- it, without needing to delete them manually first.
create table question_concept
(
    question_id bigint not null references question (id) on delete cascade,
    concept_id  bigint not null references concept (id) on delete cascade,
    primary key (question_id, concept_id)
);