-- A document is now registered before its file exists in S3, so the lifecycle gains a first state:
-- AWAITING_UPLOAD -> UNAPPROVED -> APPROVED|REJECTED -> REMOVED. Widening the check only; every
-- existing status stays valid, so no backfill is owed.
alter table document
    drop constraint document_status_check;

alter table document
    add constraint document_status_check
        check (status in ('AWAITING_UPLOAD', 'UNAPPROVED', 'APPROVED', 'REJECTED', 'REMOVED'));

-- AWAITING_UPLOAD is where every document now starts, and it is the fail-safe default: a writer that
-- omits status describes a row whose file has not arrived (which the reconcile clears up), rather
-- than one presented as uploaded and ready for review with nothing behind it.
alter table document
    alter column status set default 'AWAITING_UPLOAD';
