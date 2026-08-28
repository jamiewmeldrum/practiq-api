# CLAUDE.md — practiq-api

*Standing instructions for Claude Code. Read fully every session. Update the "Current sprint" marker as work
progresses.*

---

## 1. Behaviour rules (non-negotiable)

- **Advisory mode by default — a hard gate, not a default to reason around.** Do NOT write, create, edit, or delete
  files, and do NOT run any command that mutates the repository or the environment — no `docker compose up/down`, no
  `spotlessApply` or any formatter, no git state changes, no dependency installs, nothing with a side effect — unless
  the developer gives a per-action instruction to do that specific thing. Reading, explaining, reviewing, diagnosing,
  and read-only commands are always fine.
- **Permission is single-use and literal.** An instruction to make one change licenses exactly that change and nothing
  adjacent. It does NOT extend to formatting, running tests/verification, committing, or tidy-up done "while I'm here".
  If the licensed change leaves the tree needing formatting or a test run, say so and stop — do not do it
  uninstructed. "You didn't say not to" is not permission.
- **Never cite a location as an argument.** A reference to a line number, a decision (`D-024`), or a section (`§9`) must
  carry its substance inline — say what it says, then cite it; never cite it in place of saying it. "Per D-024" alone
  forces the developer to break off and look it up; "the serving policy forces `status=APPROVED` in the runner, never a
  caller-optional filter (D-024)" does not. The citation is a pointer for later, never the content.
- **Finish the discussion before touching anything.** When a review or exchange raises several points, resolve all of
  them in conversation before making any change. Agreement on one point earlier in the same exchange is not a starting
  gun to act on it while others are still open — hold every change until the whole thing is settled, then act as one
  batch.
- "How would I do X?" = explain the approach. "Implement X" / "write X" / "create X" = write code.
- **Code review:** point out issues directly and explain why. Suggest fixes in prose or small targeted snippets. Never
  rewrite whole files unasked.
- When you do implement: explain what each part does, why, and flag decisions the developer may want to revisit.
- The developer is an experienced backend/DevOps engineer (Java, Micronaut/Spring, AWS, Terraform) refreshing skills
  after time away. Treat them as a capable engineer re-learning. Direct answers, no padding, no over-explanation, no
  flattery.
- **The learning goal overrides speed.** This project exists to rebuild genuine skill — every line should be explainable
  in an interview. If something with learning value is being delegated, say so.
- Acceptable to generate on request without ceremony: CI YAML, docker-compose, .gitignore, README boilerplate — low
  learning value.
- A good session-end habit: when asked, review the repo against the current sprint's DoD and these conventions, and list
  gaps.
- **DoD review output (when asked to review the repo against a sprint's DoD):** for each DoD item, state not just
  pass/fail but the *mechanism* — which existing abstraction it reused vs. re-implemented, which query/method path it
  took, and which test tier exercised it. The developer relays this to a separate PM context that cannot see the repo,
  so "reuses `QuestionQuery.studentCatalogue`" is a useful fact and "enforces the serving policy" is not. Flag any place
  a shared invariant (e.g. D-024's policy object) was copied rather than reused, and any DoD line proven only by the
  happy path. This is the engineer's debrief — mechanism over outcome — and is distinct from any PR/PM summary that may
  also be requested.

## 2. What Practiq is

Adaptive learning/practice platform. Students browse **Concepts**, answer **Questions**, self-assess against mark
schemes (AI marking later). Content is ingested from uploaded documents (PDF/Word), structured and concept-labelled by
AI, then **approved by a superuser** before students ever see it. Starts with GCSE Physics (AQA); the model scales
across subjects/levels/exam boards.

Dual purpose: (1) a deliberate skills-rebuild project, (2) a public portfolio piece — clean architecture, tests, green
CI, a decision log showing judgement. **There is no demo** — the artefact is the repo and a conversation about it (
D-035). Readable code and defensible decisions beat feature count.

## 3. Architecture

**Monolith-first, split-later — deliberate.**

```
NOW:   [React] → [practiq-api] → [PostgreSQL]
                    │ (internal async ingestion job)
                    ├→ practiq-extractor (Python, HTTP on localhost)
                    ├→ S3 (LocalStack locally)
                    └→ AIService (Stub locally / Claude API)

TARGET (Phase 4+ only): API → SQS → practiq-processor → extractor Lambda → POSTs back to API.
```

- practiq-api **owns the schema (Flyway) and ALL DB writes** — forever. Future services call the API.
- Do not introduce SQS, a processor service, or Lambda packaging before Phase 4. If asked to, point at this line first.
- Sibling repos: practiq-extractor (Python parsing), practiq-frontend (React), later practiq-infrastructure (Terraform).

## 4. Stack & project config

- Java 21 · **Micronaut 4.10.x (pinned)** · Gradle **Kotlin DSL** · package root `com.practiq`
    - Micronaut 5.0 GA'd May 2026 and moved its baseline to Java 25. This project stays on 4.x/21 deliberately. Do not
      suggest upgrading to 5.0.
- PostgreSQL 16 (Docker Compose) · Micronaut Data **JPA** · Flyway (hbm2ddl off, always)
- Config format is **`application.yml` (YAML)** — reverses D-004 (see D-043). Requires `snakeyaml` on the classpath;
  Micronaut 4 doesn't bundle it, and without it `.yml` is **silently ignored** (no error). Layered profiles unchanged.
- Environment profiles: base `application.yml` is **universal-only** (app name, feature toggles like
  `reconcile.scheduler.enabled: true`, no datasource, no running config) — it does **not** represent production.
  Explicit profiles: `local` (dev loop, compose Postgres), `test` (`@MicronautTest`). **No `application-prod.yml`** —
  production is a deliberate future config; bare base is non-functional so running requires choosing an environment (no
  accidental prod). Load order: base → `application-{env}.yml` per active env → env vars → system properties. See D-050.
- Docker: **native Docker Engine (`docker-ce`) in WSL2** — not Docker Desktop. Docker Desktop Home on Windows Home
  doesn't expose its Engine API as a Unix socket into WSL2. Native `docker-ce` is installed directly in Ubuntu and
  auto-starts via systemd. Testcontainers connects via `/run/docker.sock`.
- Lombok · Bean Validation on DTOs
- LocalStack for S3 (Sprint 0.3) — same SDK, endpoint override in local/test profiles. **Presign gotchas learned — don't
  re-derive these:**
    - Each key under `micronaut.object-storage.aws` is a *separate* storage bean (`@EachProperty`, no primary). A second
      key — including `default:` — makes unqualified injection ambiguous. Only `bucket`/`enabled` are valid there;
      `endpoint` and path-style put there are silently ignored.
    - Endpoint must be `s3.localhost.localstack.cloud`, not `localhost`: S3 puts the bucket in the hostname, so
      `localhost` yields an unresolvable `documents.localhost`. The `*.localhost.localstack.cloud` public DNS wildcard
      resolves bucket subdomains to 127.0.0.1 and matches prod URL shape. Path-style works for the client but **not for
      presigning** and diverges from prod. (Depends on public DNS — breaks offline / behind rebinding-protective
      resolvers, e.g. some CI.)
    - Credentials must be **ambient env vars** (`AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`), not app config:
      `AwsS3Operations` builds its presigner internally with only region+endpoint, so the presigner resolves via the SDK
      default chain while the client uses Micronaut's provider. YAML creds satisfy the client but the presigner throws
      `Unable to load credentials`. Env vars satisfy both, and match how a deployed IAM role resolves.
    - `aws.region` is mandatory for presigning and is read from Micronaut's `Environment`; without it:
      `Pre-signed requests require 'aws.region'`.
    - **The AWS SDK ships with no default API call timeout.** An unresponsive S3 will block the caller indefinitely —
      and any DB connection it is holding — until some other limit bites (observed around 90s). Any S3 call that runs
      inside a transaction or a scheduled job (the upload reconcile does both) must set an explicit call/attempt timeout
      so a stuck object store is time-bounded, not a stalled job. Don't rely on the SDK to give up on its own.
- **App AI key env var: `PRACTIQ_ANTHROPIC_API_KEY`** — deliberately NOT `ANTHROPIC_API_KEY`, to avoid colliding with
  Claude Code's own credentials. Never committed, never in application config.
- Local DB: `jdbc:postgresql://localhost:5432/practiq`, user/pass `practiq`/`practiq_local`.

## 5. Package layout

```
com.practiq
  web/          HTTP adapter — controller, dto/request, dto/response, dto/mapper, dto/converter,
                handler (the 11 exception handlers), binder, plus AdminKeyValidator and HttpConstants
  service/      business logic (@Singleton) — UserRef, plus a package per feature (concept,
                markscheme, question, attempt, document), each with dto/request, dto/response and a
                mapper as needed; question also has a policy package
  persistence/  the *Entity types at the top level, plus converter, projection, query, repository
  storage/      S3 access (presign, put/get); no product knowledge — endpoint/expiry arrive as data
  foundation/   cross-cutting, no layer of its own — types (enums), exception, util

  Import direction (the point of the layering): web -> service -> persistence.
  persistence and storage import nothing of ours except foundation; foundation imports
  nothing of ours at all. (ai/ and ingestion/ — the AIService and the async job — land with
  the extractor/pipeline in Sprint 1.1/1.2, under service.)
```

- **Entities live in `persistence` and take an `Entity` suffix** (`ConceptEntity`, `QuestionEntity`,
  `DocumentEntity`, …); the bare noun (`Concept`, `Question`, …) is the service-layer response record. **The service
  never exposes a persisted type** — no carve-out for "flat" entities, because an exception list is what people fail to
  remember.
- **The question read path:**
  `QuestionController → QuestionService → QuestionAccessor → QuestionQueryRunner → QuestionRepository`.
    - **`QuestionQueryRunner`** (`persistence/query/question`, concrete `@Singleton`, no generic parameter, no policy)
      owns the query mechanics once — `exists(query)`, `findAll(query)`, `findPage(query, pageable)`, the specification,
      the stable `(created_at, id)` order and the two-query concept stitch. Returns `QuestionWithConceptIds` (a
      persistence read type beside the runner that builds it), never a DTO. `findOne` became `findAll` because the
      runner can no longer assume cardinality.
    - **`QuestionAccessor`** (`service/question`) is the **only** place a policy is applied. It holds a
      `QuestionQueryPolicy`, built per audience by `QuestionAccessorFactory` and injected by qualifier (
      `@Named("student")`), and it enforces the cardinality expectation — throwing if more than one row comes back. Mark
      scheme and attempts reach question visibility through this same shared accessor, never by calling
      `QuestionService` (which would be a bidirectional service dependency that fails at construction).
    - The admin review path (Sprint 1.3) is a new policy + a factory binding, not a parallel manager.
- **Accessors exist only where there is a policy to apply or a cardinality expectation to enforce.** Exactly one
  exists (`QuestionAccessor`). Concept and mark scheme use named repository finders directly; attempts build a two-field
  query in the service. Don't add an accessor where there is nothing to guard.
- **The D-024 guarantee (the student serving policy can't be bypassed) is *conventional*, not structural, and cannot be
  otherwise.** Java can't express "only this class in another package may construct you" without a module system, so the
  choice was rule-in-persistence (structural, wrong owner) or rule-in-service (right owner, conventional). The service
  won; the per-DoD integration tests are the net. It is now conventional *in the service*, not conventional *in a
  runner's name*.
- **Projections:** `QuestionConceptLinkProjection` is the only genuine projection. `LinkedQuestion` is gone — no query
  returned its shape, so it was an assembly wearing a projection's name; the runner returns `QuestionWithConceptIds`
  instead.
- **Mappers are static methods, no interface.** A method reference already satisfies `Function<E,R>` and every
  call site knows both types statically, so polymorphism has no call site. **Trigger to revisit:** the day a
  mapper needs a collaborator (e.g. resolving `{{s3:key}}` to presigned URLs) it must become an injected
  `@Singleton`.

## 6. Data model (Flyway is the source of truth)

```
exam_board:      id, name                                  -- AQA | OCR | Edexcel | Generic
specification:   id, exam_board_id, subject, level, name, version    -- level: gcse|a_level
spec_section:    id, specification_id, parent_section_id NULL, name, section_ref, sort_order
concept:         id, name, description varchar(500), created_at   -- board-agnostic, granular ("Diffraction")

spec_section_concept: spec_section_id, concept_id          -- "AQA 6.2 covers Diffraction…"
question_concept:     question_id, concept_id
note_concept:         note_id, concept_id

question:  id, version (optimistic lock), body (varchar(10000), not null — Markdown, images via {{s3:key}}, MCQ via - [ ]/- [x]),
           difficulty int (nullable, 1-5),
           type (nullable — SHORT_ANSWER|EXTENDED|MCQ),
           status (not null, default PENDING — PENDING|APPROVED|REJECTED),
           created_at
           -- NO source / source_spec columns — origin moved to its own table (see D-036)
           -- NO mark_scheme column — it's its own entity now (see D-018)
           -- no level column (see D-010) · no content jsonb (see D-009)

document:  id, s3_key (not null), filename (not null),
           source_spec (nullable — free-text provenance HINT at upload, advisory not binding),
           status (not null — enum AWAITING_UPLOAD|UNAPPROVED|APPROVED|REJECTED|REMOVED),
           version, created_at
           -- Artefact + approval gate ONLY. No processing state (see D-039).
           -- Only APPROVED docs get processed. REJECTED = refused at review;
           -- REMOVED = withdrawn (soft-delete) — distinct facts, distinct states.
           -- Never hard-deleted except by a deliberate GDPR "knife" (D-036).
           -- Lifecycle: AWAITING_UPLOAD (registered, file not yet in storage) -> UNAPPROVED
           -- (uploaded, awaiting review) -> APPROVED|REJECTED -> REMOVED. Upload is the first
           -- stage of one lifecycle, so "approved but not uploaded" is unreachable (D-044).
           -- Abandoned AWAITING_UPLOAD rows (file never arrived) are HARD-deleted by the
           -- reconcile — nothing references them, so the soft-delete rule does not apply (D-044).
           -- source_spec belongs here (artefact describing itself), NOT on question.

document_processing_job: id, document_id (FK, not null), type (not null — enum:
           QUESTION_HARVEST|NOTE_GENERATION|…), status (not null — enum
           PENDING|RUNNING|SUCCEEDED|FAILED), timestamps, created_at
           -- Record of a processing RUN over a document (see D-039), separate from
           -- the document so it isn't updated for processing reasons. One table,
           -- type-discriminated (jobs share parent+shape — NOT the per-type split
           -- origin uses). Branching workflow: harvest / note-gen / AI-gen over one
           -- APPROVED doc. job→question link is a deferred audit axis; question_origin
           -- points at the DOCUMENT, not the job.

question_origin: id, question_id (FK → question, not null, UNIQUE, ON DELETE CASCADE),
           authorship (not null — AUTHORED|EXTRACTED|GENERATED),
           document_id (FK → document, nullable, non-null IFF authorship=EXTRACTED),
           created_at
           -- One row per question, written at creation. Cold data — off the hot
           -- question row deliberately (retrieved <1% of reads). Per-type table by
           -- pattern (NoteOrigin later, same shape) — NOT a polymorphic table, so the
           -- question→origin FK cascade is DB-enforced. See D-036.
           -- Invariant (code-enforced): document_id present IFF authorship=EXTRACTED.
           -- Field named `authorship` (not kind/source) — D-041. Shipped Sprint 0.3 (tickets 2+3).

mark_scheme: id, question_id (FK, not null, UNIQUE — 1:1), version (optimistic lock),
             body (varchar(10000), not null — Markdown, {{s3:key}} refs), created_at
                           -- Separate entity from question (D-018): different edit lifecycle.
                           -- Mark schemes get tweaked in normal review; question type/status are frozen once set. Don't share a row / lock token /
                           -- contact surface across two workflows that don't move together.
                           -- Served via its own endpoint, keyed by question_id. Never inline
                           -- on the question payload.

note:      id, title, s3_key, level, status, created_at

question_attempt:           id, question_id, session_token varchar(64), attempt_body varchar(25000), created_at
                           -- NO unique constraint on (question_id, session_token).
                           -- Repeated attempts are the revision loop, not a duplicate. See D-021.
                           -- Built in Sprint 0.2. Widths (V10): attempt_body varchar(25000),
                           -- session_token varchar(64) — each mirrored by @Size + a test pin;
                           -- session_token bounded because it is client-supplied and indexed.

question_attempt_feedback: id, question_attempt_id, source (SELF|AI),
                           self_score int NULL, max_score int NULL,   -- SELF only
                           feedback TEXT NULL,                        -- AI only
                           created_at
                           -- DESIGN SETTLED, BUILD DEFERRED TO PHASE 3 (D-019 amendment).
                           -- Storing a self-score and echoing it back has no consumer until
                           -- AI marking / progress tracking. NOT created in Sprint 0.2.
                           -- `source` discriminator ⇒ CREATE whole in Phase 3, no migration owed.
                           -- One-to-many from attempt, ordered by created_at. See D-019.

-- Phase 6 only: app_user, topic_progress
```

**Flyway conventions:**

- Migrations live in `src/main/resources/db/migration/`, named `V<n>__description.sql`
- Never edit a migration that has run somewhere that matters — but the boundary is the **merge, not the commit**: on an
  unmerged feature branch, append to the newest `V<n>` file rather than adding another (a local dev database is not
  somewhere that matters). Once merged, a migration is immutable — add a new one.
- Flyway owns the schema; it never owns content data
- `src/main/resources/db/seed_local.sql` exists for manual local data loading only — it is intentionally outside
  `db/migration/` so Flyway ignores it. Load it manually via `docker exec`. It will eventually move to a dedicated
  cross-cutting demo-data repository.

Principles: questions are tagged with concepts; spec sections map to concepts; one question serves any board/spec/level
sharing the concept. Spec revisions = new specification version + new mappings; questions untouched. Synoptic
questions = multiple concepts. Prefer **explicit join entities** over @ManyToMany for the junctions. **Students only
ever see status=approved.**

## 7. API surface

```
GET  /health
GET  /api/v1/concepts                     ordered by created_at asc
GET  /api/v1/concepts/{id}                404 → {"error":"...","status":404} envelope
GET  /api/v1/questions?conceptId={id}     approved only, paginated (flat collection, not nested — see D-015)
GET  /api/v1/questions/{id}               question detail; never carries mark_scheme (own entity now, D-018). Same serving policy as catalogue → 404 (not 403) if not APPROVED+linked
GET  /api/v1/questions/{id}/mark-scheme   the mark scheme. UNGATED — no attempt required, no 403 (attempt-before-peek is a frontend nudge). Keyed by question_id, never inline on question. 404 if absent. See D-018
POST /api/v1/questions/{id}/attempts      X-Session-Token header (nested: genuine ownership, see D-019)
GET  /api/v1/questions/{id}/attempts      this session's attempts, newest first, no pagination. NO feedback inline in 0.2 — no feedback rows until Phase 3 (D-019 amendment)
--- admin: static API key header (X-Admin-Key) until Phase 6 ---
POST /api/v1/admin/documents              → register document (AWAITING_UPLOAD) + presigned upload URL
(no endpoint) upload reconcile is a DOMAIN SERVICE, not a route — uploaded -> UNAPPROVED, delete abandoned. Triggered by a guarded in-app @Scheduled stopgap now (fail-fast if enabled outside {local,test}), a Lambda adapter later (D-049). Run-resource endpoint + client /complete both gone. Real-time accelerant (D-051): S3 object-created event -> thin Lambda -> PATCH the one doc through the SAME transition; the sweep is the backstop (recovers missed events + owns abandoned-registration expiry, which no event can signal)
GET  /api/v1/admin/questions/pending
PATCH /api/v1/admin/questions/{id}          review: set status (APPROVED|REJECTED) + field edits; If-Match/version -> 409 (D-027, D-042)
PUT  /api/v1/admin/questions/{id}/concepts  replace the concept set (sub-collection — already RESTful)
```

Conventions: versioned routes, validated DTOs, correct status codes (200/201/400/404/422/500), structured error body
`{"error": "...", "status": n}` on **all** failure paths, never leak internals. Anonymous sessions = client-generated
UUID via `X-Session-Token`; no server session state.

**Request validation (applied on every route):** a path `id` below 1 → **422** (not 404); non-numeric `page`/`size` → *
*400**; out-of-range paging values → **422**; a `size` above the configured max → **422** with the ceiling named (never
silently clamped); any `sort` parameter → **422** refused (the runners impose a total order, so no client sort can be
honoured — a reordered 200 was a silent lie); a blank or over-64-char `X-Session-Token` → **422** at the controller.
Paging past the end stays a valid request with an empty envelope. Nothing consumes the API yet, so none of these break a
client. (The full 400/404/422/500 structured envelope is in place — 11 exception handlers under `web/handler`, error
shape composed once in `web/dto/mapper`.)

## 8. Ingestion pipeline (in-process for now)

upload (presigned, browser→S3 direct) → **arrival detected by a server-owned reconcile, no client signal** (D-044): an
in-app `@Scheduled` reconcile sweeps `AWAITING_UPLOAD` docs and settles each — file in storage → PATCH to UNAPPROVED;
file never arrived → the abandoned registration is removed. **This is what ships** (Sprint 0.3, PR #35). The
S3-object-created → Lambda → PATCH accelerant (same transition, sweep as backstop) is designed but **not built —
deferred, slot in when convenient, no fixed sprint**: it needs a deployed environment to run and none exists yet (
D-051). → superuser review → **APPROVED** → processing triggered off state (D-051): SNS→SQS-per-processor fan-out (
accelerant) with a reconcile-from-state backstop ("APPROVED docs with no success record of type X" = processor X's work
list) → async job (Micronaut executor): S3 download → extractor over HTTP (`POST http://localhost:8000/extract`,
multipart or S3-key payload — contract agreed in Sprint 1.2) → `AIService.structure(chunks)` →
`AIService.suggestConcepts(question)` → write questions status=pending.
Ingestion failures are flagged for the review UI — never student-facing. AI prompts must demand strict JSON; parse
defensively; validate before persisting.
**Flow control (D-051):** every stage advances by reading STATE (level-triggered reconcile = the correctness backbone,
always present); events/queues are edge accelerants that speed a single item and never carry correctness alone (edges
can be missed). Queue = at-least-once (dup-prone → idempotent consumers), and only protects a message after enqueue, so
the reconcile backstop stays. `document_processing_job` per type = audit + idempotency key + the reconcile's work list.
Open kinks for the processing build: enqueue-gap (sweep vs transactional outbox), record shape (one type-column table vs
per-processor).

**The upload reconcile (shipped, in-process — standing facts):**

- **Trigger is an in-app `@Scheduled` job — a deliberate stopgap.** Slices 3–5 (internal HTTP endpoint → trigger
  script → EventBridge schedule in Terraform) all collapsed into it, because there is no deployed environment or
  Terraform yet for anything external to trigger. Realizes D-049 (`@Scheduled` now; a Lambda adapter over the same
  domain service later, timing fluid). Fail-fast guard: enabled outside `{local,test}` → refuse to boot (D-050 — no prod
  config, so nothing runs as prod by accident).
- **Multi-instance limitation — a scaling constraint, not a bug.** The scheduler fires on *every* running instance, so
  two instances would run two overlapping reconciles and conflict. There is no cross-instance lock. Harmless while a
  single instance runs; **must be resolved before autoscaling** — it comes due with the 0.4 deployment work. Do not add
  distributed-lock machinery now; record it, defer it.
- **Work is capped per run: 100 docs, oldest-first.** Bounds transaction size so a large backlog can't produce one
  enormous transaction; oldest-first so nothing at the back of the queue starves. Each run logs how many were left
  over — that log is the evidence for whether 100 is the right number; nobody can size it correctly yet, so the number
  is provisional and the log is how it gets sized.
- **S3 calls are timeout-bounded** (see §4) so slow storage can't stall the run or pin a DB connection.

**Content data model:** Concepts and other content are NOT seeded via Flyway migrations. Content is AI-generated and
human-reviewed through the ingestion pipeline. Flyway owns schema only. Test data is generated on the fly within tests.
A separate demo-data repository is planned for manual inspection across environments (deferred).

## 9. Testing (first-class, written alongside features)

Four tiers. **A tier is an annotation that does something.** Put each test where it can observe the behaviour
it claims to verify.

| Tier        | Naming  | Wires                                                        | Boundary                            | Gradle task       | Run cadence    |
|-------------|---------|--------------------------------------------------------------|-------------------------------------|-------------------|----------------|
| Unit        | `*Test` | one class, no context                                        | all deps mocked (JUnit 5 + Mockito) | `test`            | every change   |
| Component   | `*CT`   | real web layer (routing, binding, validation, serialisation) | `@CTSlice` — **no DB**              | `test`            | every change   |
| Integration | `*IT`   | full stack                                                   | real Postgres via Testcontainers    | `integrationTest` | pre-merge / CI |
| Performance | `*PT`   | full stack                                                   | real Postgres via Testcontainers    | `performanceTest` | pre-merge / CI |

### What each tier answers

**Ask this before writing a test, and before deleting one.** Wiring tells you how a test is plugged in; only
the question tells you whether it's worth having.

| Naming          | Question it answers                                                                                                                    |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `*Test`         | Does this logic do what it should?                                                                                                     |
| `*CT`           | Does the web layer bind, serialise, and map correctly?                                                                                 |
| `*ControllerIT` | **Does the DoD actually hold, end to end?**                                                                                            |
| `*RepositoryIT` | Does querying this table behave as I think — filters, ordering, locking? One class per table (a unit test; the DB is part of the unit) |
| `*DatabaseIT`   | Does the migration say what I think it says?                                                                                           |
| `*PT`           | Is the query plan the shape I think it is?                                                                                             |

**Three IT flavours, one tier.** They share a tier because they share a *mechanism* — needs a container, runs
in `integrationTest`. They differ in the question, which the name carries. `IT` honestly means "needs Docker",
which is true of all three; "integration" is a conceptual promise the suffix never made.

- **A tier is an annotation that does something** (`@CTSlice` cuts persistence; `@PerformanceTest` sets
  `generate_statistics`, gets its own task and opt-out flag). **A flavour is a naming convention that means
  something.** Never build the first to express the second — a `@DatabaseTest` annotation would do nothing:
  same container, environment, task, cadence. See D-032.
- **Promote a flavour to a tier only when it needs mechanism** — different cadence, own opt-out, different
  wiring. Naming clarity alone never earns it.
- **No tier substitutes for another.** Repo tests prove you understand your tools; they do not prove the app
  calls them. Change `findOne(spec)` to `customQuery()` and the repo test still passes — it tests a method
  production abandoned, and it goes stale silently, without failing. Only the IT's meaning survives that
  refactor.

### Rules

- **ITs are the DoD in executable form.** An endpoint owes **one IT per clause of what you said you'd build** —
  not one per predicate, code path, or data condition. If the DoD doesn't name a case, it doesn't earn an IT.
  (D-018's DoD names both mark-scheme 404 arms, so both get one.)
- **The runner is the unit-test entry point.** A runner (e.g. `QuestionQueryRunner`) is tested as one
  collaborative unit with its **real** internal parts inside it — policy, specification factory — mocking only
  the database boundary. Internal classes are implementation details tested *through* the runner, not seams
  that each earn a test file. **A seam is not automatically a test boundary**: a test file per internal
  collaborator produces tests that assert which objects were passed between internals, break on every
  refactor, and catch nothing.
- **One `*RepositoryIT` per table.** Everything about how that table is queried — filters, ordering, locking —
  proven against real Postgres in one class. (`*SpecificationFactoryIT` is retired as a flavour and folded in.)
  **`*DatabaseIT` stays separate**: it answers what the *migration* guarantees, which is a different question
  from how the table is queried.
- **A column with a DB ceiling owes two boundary tests** — a repository/entity test proving the application `@Size`
  guard rejects an over-long value, and a `*DatabaseIT` proving the hard column cap does too. Different layers; neither
  substitutes for the other. (`concept.name`, `document.source_spec`, the `V10` widths.)
- **Standalone unit tests only where a guarantee is invisible from a higher boundary.** Kept for reusable
  components — `SessionTokenRestriction`, difficulty mapping, response mappers, the serving-policy object.
  Dropped for abstract base classes, which are proven through their concrete implementations (so each concrete
  subclass re-proves the shared mechanics — deliberate duplication, not an oversight).
- **Assert outcomes, not mechanisms.** A service test asserts the full response the caller receives,
  independent of how it was produced. Asserting the objects passed between internals is the coupling that made
  change disproportionately expensive.
- **Harnesses that lower the marginal cost of the next contract test are worth building** (e.g. `CriteriaProbe`,
  so future query restrictions get contract tests cheaply). A fixture that *bypasses* the intended path is not —
  see the runner/policy note above.
- **A new repository query owes a repo test** — not because the IT misses it, but because "do I understand this
  method" is otherwise unasked, and the repo tier drifts to testing methods production no longer calls.
- **By-id tests insert two rows with distinct, fixed, non-1 ids** and assert the correct one came back. With one
  row a test claims "you get the question you asked for" but only proves "you get *a* question". Two, not "at
  least two" — a third row proves nothing further. Fixed ids, not random — reproducibility beats coverage
  theatre. Applies to every by-id lookup, every entity, every tier.
- **DTO shape is owned by the controller CT**, asserted over a real HTTP call. ITs assert values and filtering;
  real persistence doesn't change a field list. Don't assert shape through a hand-injected `ObjectMapper` — it
  isn't necessarily the encoder the route uses.
- **`TestData` defaults mirror the DB's own defaults and mask nothing** (e.g. `QuestionRow` leaves `status` to
  the DB's `PENDING` so `QuestionDatabaseIT` can observe it). **Name a field only where the test depends on its
  value.** Builder forms: **one unconstrained form for when the test controls everything, and every other form
  must produce a valid row.** Not "one empty and one valid" — add a form when a test needs it, never to
  complete a symmetry.
- **No data setup in `setUp()`** beyond `data.clear()` and port/counter wiring. Each test inserts exactly what
  it depends on — globally-seeded data hides what a test actually relies on.
- **Test fixtures insert their own rows inline.** No test helper inserts anything — a helper may build and return a
  value, but the `insert` belongs at the call site, where the test can see what it depends on.
- **Proving a policy-bound collaborator was actually wired in.** A unit test constructs the collaborator itself, so it
  can't prove the *right* one was injected. `QuestionServiceAccessorCT` starts the real context, mocks only the
  repository, and resolves the specification the service caused through `utils.CriteriaProbe`, asserting the
  restrictions it carries. `CriteriaProbe` proves which criteria calls were made, never which rows come back — which is
  why the specification factories are proven in the repository ITs instead.
- **Statics only for values that must change together** (endpoint paths, pinned statement counts, stub
  sentinels). Test data values are per-test locals.
- **Tests never import an application constant.** A test asserting against a production constant (e.g.
  `MAX_CONTENT_LENGTH`) moves in lockstep with it — change the constant and every test still passes, having proved
  nothing; the value is untested by construction. Restate the expected value test-side, so production and test are two
  independent statements of the same rule and a change on either is a failure someone has to look at. (It earned this
  immediately: raising the cap to 25MB broke three tests that would otherwise have gone green in silence.) The one
  exception is a static import of the *method under test* — that's the subject, not an expected value.
- **Never mock entities.** Build real instances + `TestReflection.setField` for DB-assigned fields (`id`,
  `createdAt`, `version`) — a mocked entity answers whatever you stubbed and can't catch a mapper reading the
  wrong field. Reflection for anything a constructor *could* set is a missing constructor, not a convention.
- **Responsibility & overlap (honeycomb, not strict pyramid):** higher tiers are deliberately
  multi-responsibility. "One reason to fail" is a unit heuristic — do not impose it on CT/IT. Overlap is
  defence in depth, not waste. Push checks down from IT to CT where the CT can genuinely observe the thing.
- **Mutation analysis finds blind spots. It never decides deletions.** A mutation changes production code, so
  the test that fires "closest to the cause" is by construction the one most coupled to the implementation —
  it measures implementation-coupling and calls it value, ranking unit tests first and ITs last on any
  codebase. See D-032.
- **Scaffolding convention:** test stubs use `fail("not yet implemented")`, never empty bodies.
- Component tests run in the everyday `test` task and must not require Docker. `integrationTest` and
  `performanceTest` are the Docker-gated tasks.
- **Micronaut caveat:** no `@WebMvcTest`-style slice exists; the context is fairly fully wired. Component tests
  must disable persistence (Flyway/datasource/JPA) or Testcontainers pulls in Postgres and the speed benefit is
  lost. Exact mechanism to be recorded as a sub-decision to D-007 before Sprint 0.2 closes.
- StubAIService is the default for all tiers. Tests must be independent and readable.

### Performance tier specifics

`@PerformanceTest` (bundles `@MicronautTest(transactional=false, environments="performance")`) ·
`src/test/java/performance/` · `application-performance.yml` sets `hibernate.generate_statistics=true` ·
`utils/StatementCounter` wraps Hibernate `Statistics`.

- Asserts **JDBC statement counts per request, not output**. Every endpoint gets a happy-path absolute pin;
  row-scaling endpoints also get a row-count invariance test.
- **Use `getPrepareStatementCount()`, never `getQueryExecutionCount()`** — the latter doesn't see eager
  secondary selects, i.e. exactly the regression being hunted.
- **Both assertions are needed.** Invariance catches an N+1 (count scales with rows). The absolute pin catches
  a fetch-join reintroduction — which invariance sails past (count stays constant) and the correctness ITs also
  pass (in-memory paging still returns correct rows). The magic numbers are deliberate friction: update the pin
  consciously when a query is legitimately added, and no more — do NOT add a comment restating what the number is made
  of; it restates the code and goes stale, leaving a correct number with a false explanation after one refactor. Don't
  decompose it into named constants — that asserts a composition which stops being true once branching exists.
- **Current pins:** `/concepts` 1 · `/concepts/{id}` 1 · `/questions` 3 · `/questions/{id}` 2 ·
  `/questions/{id}/mark-scheme` 2 · `/questions/{id}/attempts` GET 2 (+ row-count invariance) · POST 2.
  The POST pin is 2 (visibility `exists` + the INSERT) because `@Generated` read-back of `id`/`created_at`
  rides Postgres's `insert … returning` — a third statement appearing means that mechanism broke.
- Runs in `check`/`build` by default; `-PskipPerf` opts out. `mustRunAfter(integrationTest)` — both share one
  Test Resources Postgres, and `shouldRunAfter` is only advisory (would race under `--parallel`).

### An endpoint's test tax

A new endpoint owes: **1 CT** (shape + representative values) · **1 ordering IT** (D-029 — never a fresh paging
suite) · **1 IT per DoD clause** · **1 PT pin** (+ invariance if row-scaling) · **1 `TestData` builder**. Repo
tests are owed per *new query*, not per endpoint.

## 10. Hard rules

- Constructor injection only — never field `@Inject`.
- No business logic in controllers or repositories.
- No AI calls outside the `ai/` package / AIService interface.
- Flyway only — never hbm2ddl create/update. Flyway owns schema, never content.
- **Enum-backed columns are stored upper-case** (e.g. `'PENDING'`, `'APPROVED'`), matching Java enum constant names
  exactly, so `@Enumerated(EnumType.STRING)` works with no custom `AttributeConverter`. Applies to every enum-backed
  column project-wide, not case-by-case. Free-text columns (e.g. `body`, `filename`) are unaffected.
- **Path nesting only for genuine ownership** (child meaningless outside a specific parent). **Many-to-many
  relationships or multi-dimension-filterable resources use a flat first-order collection with query-parameter filters
  **, not path nesting under one relationship. Controllers map to one entity each. See D-015.
- **`difficulty` serialises as `{value, code}`** — DB column stays a plain integer; `code` is derived at the
  serialisation layer via `TRIVIAL(1), EASY(2), MEDIUM(3), HARD(4), VERY_HARD(5)`. Whole object is `null` when
  difficulty is unrated, not partially populated. Nominal enums (`type`/`status`, and `authorship` on `question_origin`)
  serialise as their bare string code — no wrapper. See D-017.
- **Never add a unique constraint on `(question_id, session_token)`.** Repeated attempts at the same question are the
  core revision loop. Duplicates from network retries are accepted at this stage — see D-021. Idempotency keys are a
  hard prerequisite for AI grading, to be built *before* it, never as a follow-up.
- **Assessment lives in `question_attempt_feedback`, never on the attempt row.** `source` (`SELF`|`AI`) discriminates. *
  *The whole table's build is deferred to Phase 3** (D-019 amendment) — not created in Sprint 0.2. Self-assessment at
  MVP is frontend-only against the visible mark scheme; the backend stores no feedback rows yet. When it lands, `CREATE`
  it whole with both shapes — no migration owed.
- **Mark scheme is its own entity, served ungated.** Not a column on `question` (D-018).
  `GET /api/v1/questions/{id}/mark-scheme` never gates on an attempt; the "attempt before you peek" behaviour is a
  frontend nudge, never a backend 403.
- **Never fetch-join a to-many collection in a paginated query.** Hibernate silently pages in memory. Use the two-query
  stitch: paged root query, then a keyed id-pair projection, stitched onto DTOs. See D-025.
- **Use a correlated `EXISTS` subquery, not a join, to filter on a to-many.** A join multiplies rows and corrupts
  `totalCount`. See D-026.
- **The student serving policy lives in `StudentQuestionQueryPolicy` (`service/question/policy`), applied
  in `QuestionAccessor`, not in a controller filter.** It imposes `status=APPROVED` + concept-linked; request filters
  pass through. `status` is never a client parameter. Filters narrow, never widen. *(
  The `QuestionQuery.studentCatalogue(...)` named constructors are gone — removed in the D-033 refactor. Enforcement
  is **conventional**, not structural: see the D-024 note in §5.)* See D-024 as amended, D-033.
- **Write endpoints bind their payload with `@Body`, never `@RequestBean`.** `@RequestBean` binds fields individually,
  so a write endpoint would silently accept its payload as a query parameter — putting user content into access logs,
  proxy logs, browser history and `Referer` headers. `@RequestBean` stays correct for read endpoints assembling filter
  parameters. Input limits are bounded twice on purpose: the product rule on the request DTO, a backstop on the entity,
  and the two numbers stay independent (the product rule may vary by question type; the storage guard won't). See D-034.
- **Paged endpoints return `PageResponse<T>`; unpaged collections return bare arrays. Nulls always serialise.** See
  D-023.
- **Framework paging behaviour is pinned once in `PagingCT`.** A new paginated endpoint adds an *ordering* IT only —
  never a fresh paging suite. See D-029.
- **Cross-aggregate references are by scalar id, never associations — no exceptions.** `MarkSchemeEntity` holds
  `long questionId`, not `@OneToOne QuestionEntity`; no entity holds an object reference across an aggregate boundary.
  The relationship is expressed once — the FK and its DB constraint. Navigation is a repository query by id. `@OneToOne`
  defaults to EAGER and its inverse can't be lazy without bytecode enhancement, so both directions fire wasted selects.
  **The former `QuestionEntity.conceptLinks` `@OneToMany` carve-out was deleted** — it had zero readers and its cascade
  merely duplicated the database's own `ON DELETE CASCADE`. See D-031.
- **Entities never carry value defaults on fields.** An entity's initial state is set by a full-body constructor or a
  named factory method; the no-arg constructor is protected (Hibernate's only). A field initialiser makes "I forgot to
  set this" and "I meant this value" indistinguishable, and silently owns a decision that a second creation path would
  want to make for itself. **Collection initialisers are not value defaults and stay** — an unset collection is an NPE,
  not a lifecycle decision. **The DB column default stays and serves a different caller:** Hibernate lists every mapped
  column in its INSERT, so a column default is never reachable from application writes — it guards non-JPA writers
  only (`TestData`, seed scripts) and is pinned by the table's `*DatabaseIT`. *Applied across every entity:* the
  bare-noun value defaults are gone; each entity has a protected no-arg constructor (Hibernate's only), a private
  populating constructor and named factory methods. `DocumentEntity` and `QuestionOriginEntity` are compliant too.
- **Entities take an `Entity` suffix and live in `persistence`; the bare noun is the service-layer response record.**
  The service never returns a persisted type. **`version` is `int` everywhere** — column, entity and service record —
  and it never reaches a web response.
- **Origin is a separate per-type table, never columns on the content.** A question's provenance (`AUTHORED`/
  `EXTRACTED`/`GENERATED`, and the source document if extracted) lives in `question_origin`, not on `question`. Reasons:
  origin is *cold* (read <1% of the time — keep it off the hot catalogue row) and *polymorphic in shape but not in
  table* (`NoteOrigin` later, same columns, its own hard FK). Per-type tables, **shared writing logic in code** (an
  origin-writer the extraction job and manual authoring both call) — not a shared polymorphic table, so the
  content→origin FK cascade stays DB-enforced. `document_id` is non-null **iff** `authorship=EXTRACTED` — the field is
  named `authorship`, not `kind` (D-041). Generation is diffuse and unattributable; authoring has no document. Documents
  are never hard-deleted outside a deliberate GDPR procedure, so that invariant holds structurally. See D-036.
- **Read projections are distinct types from write entities.** `QuestionConceptLink` (projection) ≠ `QuestionConcept` (
  entity). Don't reuse the entity where a lean projection is what the read path needs. See D-030.
- No secrets in code, config, or commits.
- **No copyrighted material committed — ever.** Real past papers may be used privately for local testing; all committed
  fixtures/seed content are self-written. Seed concept lists may mirror the published spec's topic structure (facts),
  not its prose.
- Commit small and often, meaningful messages — the public history is CV evidence.
- Don't build Phase 4+ infrastructure early (SQS/processor/Lambda/Terraform).

## 11. Plan & current position

Sprint sequence: **0.1** skeleton + Concept endpoint + CI *(complete)* → **0.2** question read API + attempts *(
complete)* → **0.3** ingestion (stub AI) → **0.4** deployed environment — correct topology in Terraform, run
ephemerally (spin up / test / tear down on a timer) so it costs pennies; CI automated, CD manual (pulled forward,
D-035) → **1.1** extractor (separate repo) → **1.2** upload + pipeline → **1.3** review API → **2.1** student UI *(
non-load-bearing — delegate or skip)* → **2.2** superuser UI → **2.3** repo hardening → **3** AI marking → **4** service
extraction → **5** infra depth (RDS/persistence, autoscaling, EC2→Fargate) → **6** accounts/progress/generation. **There
is no demo — the artefact is the repo + a conversation (D-035).**

> **Current sprint: 0.3 — Ingestion (stub AI, in-process)** *(update this line as sprints complete)*
>
> **Sprint 0.2 closed.** Question read API, mark scheme, and attempts all delivered.
>
> **Sprint 0.3 — document upload piece COMPLETE** (PR #35, 16 commits, green across all four tiers). Schema done (
`document`, `question_origin`; `source`/`source_spec` dropped — D-036/D-041). Slices 1–2: LocalStack S3 + CI wiring;
> presigned-upload endpoint + `AWAITING_UPLOAD` record (status-default defect fixed via the create-path constructor). *
*Layering refactor merged (PR #32):** `web`/`service`/`persistence`/`storage`/`foundation`; `Entity` suffix, bare noun =
> service record; per-route validation (id<1→422, bad paging→400/422, `sort` refused, session-token bounds→422). **Upload
lifecycle now closed:** the reconcile **domain service** (no HTTP endpoint — D-049) driven by an in-app `@Scheduled`
> stopgap settles every `AWAITING_UPLOAD` doc (arrived → UNAPPROVED; never arrived → removed), so a failed browser upload
> no longer leaves a permanent orphan row. Batch-capped (100, oldest-first, logs leftover), S3 calls timeout-bounded,
> config restructured to no-prod (D-050). Full reconcile standing facts + the multi-instance constraint live in §8.
>
> **Deferred — slot in soon, no fixed sprint:** the S3-object-created → Lambda → PATCH accelerant (D-051, same
> transition, sweep as backstop) — designed, not built; it needs a deployed environment to run, so it waits on infra
> rather than on a sprint boundary. **Comes due with 0.4:** the multi-instance scheduler fix (the `@Scheduled` stopgap
> fires on every instance — §8).
>
> **Next functional work:** review/approve `PATCH` (1.3 — see the 409 note below), then extraction + Flow 2 processing (
> SNS→SQS-per-processor + reconcile-from-state backstop, two open kinks — D-051).
>
> **Done:** `question`/`question_concept` migrations · `@Version` on content entities · `GET /api/v1/questions` (paged,
> filterable, serving policy enforced) · full 400/404/422/500 error envelope · `PageResponse<T>` · two-query concept
> stitch · JPA static metamodel · `GET /api/v1/questions/{id}` · `mark_scheme` entity + `V3__mark_scheme.sql` +
`GET /api/v1/questions/{id}/mark-scheme` (ungated) — **D-018 closed in code** · `QuestionQueryRunner`/
`StudentQuestionQueryPolicy` + `QuestionWithConceptIds` · performance tier (
`*PT`) · `question_attempt` table + `GET /api/v1/questions/{id}/attempts` (per-session, newest-first, visibility-gated
> via the runner) · `X-Session-Token` handling (absent → 400, blank → 422) · `POST /api/v1/questions/{id}/attempts` (201 +
> created attempt, `@Body`-bound, visibility-gated via the same runner `exists`; body length 20k on the DTO, 25k entity +
> column cap (V10)) — **attempts feature complete**.
>
> **Build & CI workstream** (cross-cutting — D-037/D-038): merge-to-main pipeline, security scanning, badge, timeouts
> all shipped. **CodeQL caveat (D-038):** no Micronaut taint model, so injection-class flaws in controllers are *not*
> detected — verified, documented, not full coverage; lean on secret scanning + dependency monitoring. One item left: the
> carried CT persistence-disable D-007 write-up. **Sprint 0.3 (`document` table onward) is the main line.**
>
> **409 handler for `OptimisticLockException` moved to Sprint 1.3** — owed with the first write against a *versioned*
> entity (the admin question-review `PATCH` — D-042). `question_attempt` has no `@Version`, so the attempts POST cannot
> produce a conflict (D-027 as corrected).
>
> **Deliberately out of scope:** `question_attempt_feedback` — whole table deferred to Phase 3 (D-019 amendment; no
> consumer for a self-score yet) · MCQ auto-marking (separate feature, unscheduled) · idempotency keys (D-021) · unique
> constraint on attempts (would break the revision loop) · server-issued session tokens (D-020) · async grading / `202` /
> polling · batch submission · strict `Pageable` binder (D-028) · the attempt body cap is now `varchar(25000)` at the
> column (V10) with a matching `@Size(max = 25000)` on the entity — the earlier "a loose `CHECK` owes a migration only if
> non-JPA writers appear" caveat no longer applies, since the column carries the cap (D-034).

Full sprint briefs live in PRACTIQ_MASTER.md (the planning doc, kept outside the repo). If a sprint brief is pasted, it
governs the session's scope.