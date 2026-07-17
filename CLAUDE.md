# CLAUDE.md — practiq-api
*Standing instructions for Claude Code. Read fully every session. Update the "Current sprint" marker as work progresses.*

---

## 1. Behaviour rules (non-negotiable)

- **Advisory mode by default.** Do NOT write, create, edit, or delete files, and do NOT run state-changing commands, unless explicitly asked for implementation. Reading, explaining, reviewing, and diagnosing are always fine.
- "How would I do X?" = explain the approach. "Implement X" / "write X" / "create X" = write code.
- **Code review:** point out issues directly and explain why. Suggest fixes in prose or small targeted snippets. Never rewrite whole files unasked.
- When you do implement: explain what each part does, why, and flag decisions the developer may want to revisit.
- The developer is an experienced backend/DevOps engineer (Java, Micronaut/Spring, AWS, Terraform) refreshing skills after time away. Treat them as a capable engineer re-learning. Direct answers, no padding, no over-explanation, no flattery.
- **The learning goal overrides speed.** This project exists to rebuild genuine skill — every line should be explainable in an interview. If something with learning value is being delegated, say so.
- Acceptable to generate on request without ceremony: CI YAML, docker-compose, .gitignore, README boilerplate — low learning value.
- A good session-end habit: when asked, review the repo against the current sprint's DoD and these conventions, and list gaps.
- **DoD review output (when asked to review the repo against a sprint's DoD):** for each DoD item, state not just pass/fail but the *mechanism* — which existing abstraction it reused vs. re-implemented, which query/method path it took, and which test tier exercised it. The developer relays this to a separate PM context that cannot see the repo, so "reuses `QuestionQuery.studentCatalogue`" is a useful fact and "enforces the serving policy" is not. Flag any place a shared invariant (e.g. D-024's policy object) was copied rather than reused, and any DoD line proven only by the happy path. This is the engineer's debrief — mechanism over outcome — and is distinct from any PR/PM summary that may also be requested.

## 2. What Practiq is

Adaptive learning/practice platform. Students browse **Concepts**, answer **Questions**, self-assess against mark schemes (AI marking later). Content is ingested from uploaded documents (PDF/Word), structured and concept-labelled by AI, then **approved by a superuser** before students ever see it. Starts with GCSE Physics (AQA); the model scales across subjects/levels/exam boards.

Dual purpose: (1) a deliberate skills-rebuild project, (2) a public portfolio piece — clean architecture, tests, green CI, 5-minute demo. Working demo beats feature count.

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
  - Micronaut 5.0 GA'd May 2026 and moved its baseline to Java 25. This project stays on 4.x/21 deliberately. Do not suggest upgrading to 5.0.
- PostgreSQL 16 (Docker Compose) · Micronaut Data **JPA** · Flyway (hbm2ddl off, always)
- Config format is **`application.properties`**, not YAML — Micronaut 4's default. Don't convert to `.yml`.
- Environment profiles: `local` for dev loop (compose Postgres), `test` activated by `@MicronautTest`. Base `application.properties` contains no datasource URL — environments provide it. This prevents tests accidentally hitting the dev database.
- Docker: **native Docker Engine (`docker-ce`) in WSL2** — not Docker Desktop. Docker Desktop Home on Windows Home doesn't expose its Engine API as a Unix socket into WSL2. Native `docker-ce` is installed directly in Ubuntu and auto-starts via systemd. Testcontainers connects via `/run/docker.sock`.
- Lombok · Bean Validation on DTOs
- LocalStack for S3 (added Sprint 1.2) — same SDK, endpoint override in local profile
- **App AI key env var: `PRACTIQ_ANTHROPIC_API_KEY`** — deliberately NOT `ANTHROPIC_API_KEY`, to avoid colliding with Claude Code's own credentials. Never committed, never in application config.
- Local DB: `jdbc:postgresql://localhost:5432/practiq`, user/pass `practiq`/`practiq_local`.

## 5. Package layout

```
com.practiq
  api/                REST controllers (thin — no business logic)
  service/            business logic (@Singleton) + QuestionQueryManager
  domain/             JPA entities
  domain/projection/  read projections (LinkedQuestion, QuestionConceptLink) — never entities
  domain/query/       QuestionQuery + specification factory
  repository/         Micronaut Data repositories
  ai/                 AIService interface + StubAIService + ClaudeAIService
  ingestion/          async ingestion job + extractor HTTP client (Sprint 1.2+)
  config/             configuration
  dto/                request/response records — never expose entities
  dto/mapper/         static mapper methods (ConceptResponseMapper, QuestionResponseMapper, …)
```

- **`QuestionQueryManager` owns question retrieval.** Question repository calls live there, not in services. A
  service that needs question data goes through it. It holds the `studentCatalogue` policy, the stable
  `(created_at, id)` ordering, and the two-query concept stitch; it returns domain projections, never DTOs —
  services do the DTO mapping. This does **not** supersede D-024: the policy is still an invariant on
  `QuestionQuery`'s constructor. Two mechanisms, deliberately.
- **`LinkedQuestion`** (`domain/projection/`) — `Question` + `Set<QuestionConceptLink>`; the read path's
  assembly type, consumed by `QuestionResponseMapper`.
- **Mappers are static methods, no interface.** A method reference already satisfies `Function<E,R>` and every
  call site knows both types statically, so polymorphism has no call site. **Trigger to revisit:** the day a
  mapper needs a collaborator (e.g. resolving `{{s3:key}}` to presigned URLs) it must become an injected
  `@Singleton`.

## 6. Data model (Flyway is the source of truth)

```
exam_board:      id, name                                  -- AQA | OCR | Edexcel | Generic
specification:   id, exam_board_id, subject, level, name, version    -- level: gcse|a_level
spec_section:    id, specification_id, parent_section_id NULL, name, section_ref, sort_order
concept:         id, name, description, created_at         -- board-agnostic, granular ("Diffraction")

spec_section_concept: spec_section_id, concept_id          -- "AQA 6.2 covers Diffraction…"
question_concept:     question_id, concept_id
note_concept:         note_id, concept_id

question:  id, version (optimistic lock), body (TEXT, not null — Markdown, images via {{s3:key}}, MCQ via - [ ]/- [x]),
           difficulty int (nullable, 1-5),
           type (nullable — SHORT_ANSWER|EXTENDED|MCQ),
           source (not null — SEED|EXTRACTED|GENERATED, see D-014),
           status (not null, default PENDING — PENDING|APPROVED|REJECTED),
           source_spec (VARCHAR, nullable — provenance stopgap, see D-010),
           created_at
           -- NO mark_scheme column — it's its own entity now (see D-018)
           -- no level column (see D-010) · no content jsonb (see D-009)
           -- source_document_id FK deferred to Sprint 0.3 (see D-014)

mark_scheme: id, question_id (FK, not null, UNIQUE — 1:1), version (optimistic lock),
             body (TEXT, not null — Markdown, {{s3:key}} refs), created_at
                           -- Separate entity from question (D-018): different edit lifecycle.
                           -- Mark schemes get tweaked in normal review; question type/source/
                           -- status are frozen once set. Don't share a row / lock token /
                           -- contact surface across two workflows that don't move together.
                           -- Served via its own endpoint, keyed by question_id. Never inline
                           -- on the question payload.

note:      id, title, s3_key, level, status, created_at

question_attempt:           id, question_id, session_token, attempt_body, created_at
                           -- NO unique constraint on (question_id, session_token).
                           -- Repeated attempts are the revision loop, not a duplicate. See D-021.
                           -- Built in Sprint 0.2.

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
- Never edit a migration that has already been applied — add a new one
- Flyway owns the schema; it never owns content data
- `src/main/resources/db/seed_local.sql` exists for manual local data loading only — it is intentionally outside `db/migration/` so Flyway ignores it. Load it manually via `docker exec`. It will eventually move to a dedicated cross-cutting demo-data repository.

Principles: questions are tagged with concepts; spec sections map to concepts; one question serves any board/spec/level sharing the concept. Spec revisions = new specification version + new mappings; questions untouched. Synoptic questions = multiple concepts. Prefer **explicit join entities** over @ManyToMany for the junctions. **Students only ever see status=approved.**

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
POST /api/v1/admin/documents              → presigned S3 URL
POST /api/v1/admin/documents/{id}/complete → triggers async ingestion job
GET  /api/v1/admin/questions/pending
PUT  /api/v1/admin/questions/{id}/approve | /reject | /concepts
```
Conventions: versioned routes, validated DTOs, correct status codes (200/201/400/404/422/500), structured error body `{"error": "...", "status": n}` on **all** failure paths, never leak internals. Anonymous sessions = client-generated UUID via `X-Session-Token`; no server session state.

**Outstanding (Sprint 0.1):** only 404 currently returns the structured envelope. 400/422/500 need a global exception handler before the DoD is met.

## 8. Ingestion pipeline (in-process for now)

upload (presigned, browser→S3 direct) → complete notification → async job (Micronaut executor): S3 download → extractor over HTTP (`POST http://localhost:8000/extract`, multipart or S3-key payload — contract agreed in Sprint 1.2) → `AIService.structure(chunks)` → `AIService.suggestConcepts(question)` → write questions status=pending → superuser review → approve.
Ingestion failures are flagged for the review UI — never student-facing. AI prompts must demand strict JSON; parse defensively; validate before persisting.

**Content data model:** Concepts and other content are NOT seeded via Flyway migrations. Content is AI-generated and human-reviewed through the ingestion pipeline. Flyway owns schema only. Test data is generated on the fly within tests. A separate demo-data repository is planned for manual inspection across environments (deferred).

## 9. Testing (first-class, written alongside features)

Four tiers. **A tier is an annotation that does something.** Put each test where it can observe the behaviour
it claims to verify.

| Tier | Naming | Wires | Boundary | Gradle task | Run cadence |
|------|--------|-------|----------|-------------|-------------|
| Unit | `*Test` | one class, no context | all deps mocked (JUnit 5 + Mockito) | `test` | every change |
| Component | `*CT` | real web layer (routing, binding, validation, serialisation) | `@CTSlice` — **no DB** | `test` | every change |
| Integration | `*IT` | full stack | real Postgres via Testcontainers | `integrationTest` | pre-merge / CI |
| Performance | `*PT` | full stack | real Postgres via Testcontainers | `performanceTest` | pre-merge / CI |

### What each tier answers

**Ask this before writing a test, and before deleting one.** Wiring tells you how a test is plugged in; only
the question tells you whether it's worth having.

| Naming | Question it answers |
|--------|---------------------|
| `*Test` | Does this logic do what it should? |
| `*CT` | Does the web layer bind, serialise, and map correctly? |
| `*ControllerIT` | **Does the DoD actually hold, end to end?** |
| `*RepositoryIT` / `*SpecificationFactoryIT` | Do I understand the method I'm calling? (a unit test — the DB is part of the unit) |
| `*DatabaseIT` | Does the migration say what I think it says? |
| `*PT` | Is the query plan the shape I think it is? |

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
- **Statics only for values that must change together** (endpoint paths, pinned statement counts, stub
  sentinels). Test data values are per-test locals.
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
`src/test/java/performance/` · `application-performance.properties` sets `hibernate.generate_statistics=true` ·
`utils/StatementCounter` wraps Hibernate `Statistics`.

- Asserts **JDBC statement counts per request, not output**. Every endpoint gets a happy-path absolute pin;
  row-scaling endpoints also get a row-count invariance test.
- **Use `getPrepareStatementCount()`, never `getQueryExecutionCount()`** — the latter doesn't see eager
  secondary selects, i.e. exactly the regression being hunted.
- **Both assertions are needed.** Invariance catches an N+1 (count scales with rows). The absolute pin catches
  a fetch-join reintroduction — which invariance sails past (count stays constant) and the correctness ITs also
  pass (in-memory paging still returns correct rows). The magic numbers are deliberate friction: update the pin
  consciously when a query is legitimately added, with a comment saying what the number is made of. Don't
  decompose it into named constants — that asserts a composition which stops being true once branching exists.
- **Current pins:** `/concepts` 1 · `/concepts/{id}` 1 · `/questions` 3 · `/questions/{id}` 2 ·
  `/questions/{id}/mark-scheme` 2.
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
- **Enum-backed columns are stored upper-case** (e.g. `'PENDING'`, `'APPROVED'`), matching Java enum constant names exactly, so `@Enumerated(EnumType.STRING)` works with no custom `AttributeConverter`. Applies to every enum-backed column project-wide, not case-by-case. Free-text columns (e.g. `source_spec`) are unaffected.
- **Path nesting only for genuine ownership** (child meaningless outside a specific parent). **Many-to-many relationships or multi-dimension-filterable resources use a flat first-order collection with query-parameter filters**, not path nesting under one relationship. Controllers map to one entity each. See D-015.
- **`difficulty` serialises as `{value, code}`** — DB column stays a plain integer; `code` is derived at the serialisation layer via `TRIVIAL(1), EASY(2), MEDIUM(3), HARD(4), VERY_HARD(5)`. Whole object is `null` when difficulty is unrated, not partially populated. Nominal enums (`type`/`source`/`status`) serialise as their bare string code — no wrapper. See D-017.
- **Never add a unique constraint on `(question_id, session_token)`.** Repeated attempts at the same question are the core revision loop. Duplicates from network retries are accepted at this stage — see D-021. Idempotency keys are a hard prerequisite for AI grading, to be built *before* it, never as a follow-up.
- **Assessment lives in `question_attempt_feedback`, never on the attempt row.** `source` (`SELF`|`AI`) discriminates. **The whole table's build is deferred to Phase 3** (D-019 amendment) — not created in Sprint 0.2. Self-assessment at MVP is frontend-only against the visible mark scheme; the backend stores no feedback rows yet. When it lands, `CREATE` it whole with both shapes — no migration owed.
- **Mark scheme is its own entity, served ungated.** Not a column on `question` (D-018). `GET /api/v1/questions/{id}/mark-scheme` never gates on an attempt; the "attempt before you peek" behaviour is a frontend nudge, never a backend 403.
- **Never fetch-join a to-many collection in a paginated query.** Hibernate silently pages in memory. Use the two-query stitch: paged root query, then a keyed id-pair projection, stitched onto DTOs. See D-025.
- **Use a correlated `EXISTS` subquery, not a join, to filter on a to-many.** A join multiplies rows and corrupts `totalCount`. See D-026.
- **The student serving policy is an object invariant, not a filter.** `QuestionQuery.studentCatalogue(...)` bakes in `status=APPROVED` + concept-linked. `status` is never a client parameter. Filters narrow, never widen. See D-024.
- **Paged endpoints return `PageResponse<T>`; unpaged collections return bare arrays. Nulls always serialise.** See D-023.
- **Framework paging behaviour is pinned once in `PagingCT`.** A new paginated endpoint adds an *ordering* IT only — never a fresh paging suite. See D-029.
- **Cross-aggregate references are by scalar id, never associations.** `MarkScheme` holds `long questionId`, not `@OneToOne Question`; `Question` holds no reference to `MarkScheme` at all. The relationship is expressed exactly once — the `mark_scheme.question_id` FK and its DB constraint. Navigation is a repository query by id. `@OneToOne` defaults to EAGER and its inverse side can't be lazy without bytecode enhancement, so both directions fire wasted selects. **Carve-out:** `Question.conceptLinks` `@OneToMany` stays — that's *within* the aggregate, it's lazy, and the admin concepts write path needs it. See D-031.
- **Read projections are distinct types from write entities.** `QuestionConceptLink` (projection) ≠ `QuestionConcept` (entity). Don't reuse the entity where a lean projection is what the read path needs. See D-030.
- No secrets in code, config, or commits.
- **No copyrighted material committed — ever.** Real past papers may be used privately for local testing; all committed fixtures/seed content are self-written. Seed concept lists may mirror the published spec's topic structure (facts), not its prose.
- Commit small and often, meaningful messages — the public history is CV evidence.
- Don't build Phase 4+ infrastructure early (SQS/processor/Lambda/Terraform).

## 11. Plan & current position

Sprint sequence: **0.1** skeleton + Concept endpoint + CI *(complete)* → **0.2** question read API + attempts (self-assessed) → **0.3** ingestion (stub AI) → **1.1** extractor (separate repo) → **1.2** upload + pipeline → **1.3** review API → **2.1** student UI → **2.2** superuser UI → **2.3** demo hardening = **demoable cut** → 3 AI marking → 4 service extraction → 5 AWS → 6 accounts/progress/generation.

> **Current sprint: 0.2 — Question read API + attempts (self-assessed)** *(update this line as sprints complete)*
>
> **Done:** `question`/`question_concept` migrations · `@Version` on content entities · `GET /api/v1/questions` (paged, filterable, serving policy enforced) · full 400/404/422/500 error envelope · `PageResponse<T>` · two-query concept stitch · JPA static metamodel · `GET /api/v1/questions/{id}` · `mark_scheme` entity + `V3__mark_scheme.sql` + `GET /api/v1/questions/{id}/mark-scheme` (ungated) — **D-018 closed in code** · `QuestionQueryManager` + `LinkedQuestion` projection · performance tier (`*PT`).
>
> **Remaining:** `question_attempt` migration/entity/repository/service (feedback table deferred to Phase 3) · `POST`/`GET /api/v1/questions/{id}/attempts` · **409 handler for `OptimisticLockException`** (owed with the first write endpoint) · `X-Session-Token` handling · write up CT persistence-disable as a D-007 sub-decision.
>
> **Deliberately out of scope:** `question_attempt_feedback` — whole table deferred to Phase 3 (D-019 amendment; no consumer for a self-score yet) · MCQ auto-marking (separate feature, unscheduled) · idempotency keys (D-021) · unique constraint on attempts (would break the revision loop) · server-issued session tokens (D-020) · async grading / `202` / polling · batch submission · strict `Pageable` binder (D-028).

Full sprint briefs live in PRACTIQ_MASTER.md (the planning doc, kept outside the repo). If a sprint brief is pasted, it governs the session's scope.