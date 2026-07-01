-- Questions served to students once approved. Body and mark_scheme are Markdown,
-- with images embedded via {{s3:key}} refs and MCQ choices via - [ ] / - [x] lists
-- (see D-009). No level column — serving context comes from the concept mapping,
-- not the question itself (see D-010). source_spec is a provenance stopgap until
-- a specification FK exists.
create table question (
    id           bigint generated always as identity primary key,
    body         text not null,
    mark_scheme  text,
    difficulty   integer not null check (difficulty between 1 and 5),
    type         varchar(20) not null,
    source       varchar(20) not null,
    status       varchar(20) not null default 'pending',
    source_spec  varchar(255),
    created_at   timestamptz not null default now()
);

-- Join table linking questions to the concepts they cover. A question can cover
-- multiple concepts (synoptic questions); a concept can be covered by many questions.
-- ON DELETE CASCADE: a link row has no meaning without both sides existing, so
-- removing either a question or a concept should remove the links that reference
-- it, without needing to delete them manually first.
create table question_concept (
    question_id bigint not null references question(id) on delete cascade,
    concept_id  bigint not null references concept(id) on delete cascade,
    primary key (question_id, concept_id)
);