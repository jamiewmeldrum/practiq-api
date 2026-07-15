-- Questions served to students once approved.
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