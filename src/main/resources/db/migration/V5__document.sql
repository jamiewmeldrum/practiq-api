create table document
(
    id          bigint generated always as identity primary key,
    version     integer     not null default 0,
    s3_key      text        not null unique,
    filename    text        not null,
    source_spec varchar(255),
    status      varchar(20) not null default 'UNAPPROVED' check (status in ('UNAPPROVED', 'APPROVED', 'REJECTED', 'REMOVED')),
    created_at  timestamptz not null default now()
);