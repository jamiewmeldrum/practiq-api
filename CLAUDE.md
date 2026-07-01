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
  api/         REST controllers (thin — no business logic)
  service/     business logic (@Singleton)
  domain/      JPA entities
  repository/  Micronaut Data repositories
  ai/          AIService interface + StubAIService + ClaudeAIService
  ingestion/   async ingestion job + extractor HTTP client (Sprint 1.2+)
  config/      configuration
  dto/         request/response records — never expose entities
```

## 6. Data model (Flyway is the source of truth)

```
exam_board:      id, name                                  -- AQA | OCR | Edexcel | Generic
specification:   id, exam_board_id, subject, level, name, version    -- level: gcse|a_level
spec_section:    id, specification_id, parent_section_id NULL, name, section_ref, sort_order
concept:         id, name, description, created_at         -- board-agnostic, granular ("Diffraction")

spec_section_concept: spec_section_id, concept_id          -- "AQA 6.2 covers Diffraction…"
question_concept:     question_id, concept_id
note_concept:         note_id, concept_id

question:  id, body, mark_scheme, difficulty int (1-5),
           type (short_answer|extended|mcq), level (gcse|a_level|both),
           source (seed|generated), status (pending|approved|rejected),
           content jsonb,                                   -- type-specific: MCQ choices, hints
           created_at

note:      id, title, s3_key, level, status, created_at

attempt:   id, question_id, session_token, answer_text,
           self_score int NULL, max_score int,
           marking_method (self|ai), feedback NULL,         -- feedback = AI marking only
           created_at

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
GET  /api/v1/concepts/{id}                404 → {"error","status"} envelope
GET  /api/v1/concepts/{id}/questions      approved only, paginated
GET  /api/v1/questions/{id}               mark scheme gated until session has attempted
POST /api/v1/attempts                     X-Session-Token header
--- admin: static API key header (X-Admin-Key) until Phase 6 ---
POST /api/v1/admin/documents              → presigned S3 URL
POST /api/v1/admin/documents/{id}/complete → triggers async ingestion job
GET  /api/v1/admin/questions/pending
PUT  /api/v1/admin/questions/{id}/approve | /reject | /concepts
```
Conventions: versioned routes, validated DTOs, correct status codes (200/201/400/404/422/500), structured error body `{"error": "...", "status": n}`, never leak internals. Anonymous sessions = client-generated UUID via `X-Session-Token`; no server session state.

## 8. Ingestion pipeline (in-process for now)

upload (presigned, browser→S3 direct) → complete notification → async job (Micronaut executor): S3 download → extractor over HTTP (`POST http://localhost:8000/extract`, multipart or S3-key payload — contract agreed in Sprint 1.2) → `AIService.structure(chunks)` → `AIService.suggestConcepts(question)` → write questions status=pending → superuser review → approve.
Ingestion failures are flagged for the review UI — never student-facing. AI prompts must demand strict JSON; parse defensively; validate before persisting.

**Content data model:** Concepts and other content are NOT seeded via Flyway migrations. Content is AI-generated and human-reviewed through the ingestion pipeline. Flyway owns schema only. Test data is generated on the fly within tests. A separate demo-data repository is planned for manual inspection across environments (deferred).

## 9. Testing (first-class, written alongside features)

Three-tier model — put each test where it can actually observe the behaviour it claims to verify:

| Tier | Naming | Wires | Boundary | Gradle task | Run cadence |
|------|--------|-------|----------|-------------|-------------|
| Unit | `*Test` | one class, no context | all deps mocked (JUnit 5 + Mockito) | `test` | every change |
| Component | `*CT` | real web layer (routing, binding, validation, serialisation) | `@MockBean` — **no DB** | `test` | every change |
| Integration | `*IT` | full stack | real Postgres via Testcontainers | `integrationTest` | pre-merge / CI |

- **Unit:** service logic; non-trivial validation rule correctness via a bare `jakarta.validation.Validator`.
- **Component:** controller behaviour — that routes map correctly, body binding works, `@Valid` is actually wired (bad payload → 400), serialisation produces the right JSON shape. Persistence boundary mocked. This is the tier that keeps ITs thin and avoids IT bloat. Cover *representative* shape/value, not exhaustive field nuance — e.g. a read endpoint gets ~2 tests (a few entities returned with the expected fields by shape and/or value, plus an empty case), not one per field.
- **Integration:** only what needs a real DB — repository queries, `@Query`, migration correctness, transactional behaviour. Kept deliberately few.
- **Responsibility & overlap (honeycomb, not strict pyramid):** higher tiers are deliberately multi-responsibility. A unit test isolates one behaviour; an IT verifies whole paths (routing + mapping + serialisation + SQL + schema at once); the CT is the middle that carries application behaviour *without* a DB. "One reason to fail" is a unit heuristic — do not impose it on CT/IT. Overlap is defense in depth, not waste: a CT may assert serialisation even when a unit test also does. Verify behaviour at the cheapest tier that can still see it — push checks down from IT to CT (faster, more reliable, sharper failure localisation) so ITs stay few and cover only what genuinely needs Postgres. Exhaustive per-field serialisation nuance (formats, nulls, edge cases) lives in small unit tests, and only when there is real nuance — don't manufacture it.
- Component tests run in the everyday `test` task alongside unit tests. They must not require Docker. `integrationTest` is the Docker-gated task.
- **Micronaut caveat:** no `@WebMvcTest`-style slice exists; the context is fairly fully wired. Component tests must disable the persistence layer (Flyway/datasource/JPA) to prevent Testcontainers pulling in a Postgres container and losing the speed benefit. Exact disable mechanism still being confirmed in practice.
- **Scaffolding convention:** test stubs use `fail("not yet implemented")`, never empty bodies — unwritten coverage shows red, not misleading green.
- StubAIService is the default for all tiers. Tests must be independent and readable.

## 10. Hard rules

- Constructor injection only — never field `@Inject`.
- No business logic in controllers or repositories.
- No AI calls outside the `ai/` package / AIService interface.
- Flyway only — never hbm2ddl create/update. Flyway owns schema, never content.
- No secrets in code, config, or commits.
- **No copyrighted material committed — ever.** Real past papers may be used privately for local testing; all committed fixtures/seed content are self-written. Seed concept lists may mirror the published spec's topic structure (facts), not its prose.
- Commit small and often, meaningful messages — the public history is CV evidence.
- Don't build Phase 4+ infrastructure early (SQS/processor/Lambda/Terraform).

## 11. Plan & current position

Sprint sequence: **0.1** skeleton + Concept endpoint + CI → **0.2** full schema + read API + attempts → **0.3** ingestion (stub AI) → **1.1** extractor (separate repo) → **1.2** upload + pipeline → **1.3** review API → **2.1** student UI → **2.2** superuser UI → **2.3** demo hardening = **interview-ready cut** → (bonus) 3 AI marking → 4 service extraction → 5 AWS → 6 accounts/progress/generation.

> **Current sprint: 0.2 — Full schema + read API + attempts** *(update this line as sprints complete)*
>
> Sprint 0.2 DoD: see PRACTIQ_MASTER.md for the brief. (0.1 — skeleton + Concept endpoint + CI — complete.)

Full sprint briefs live in PRACTIQ_MASTER.md (the planning doc, kept outside the repo). If a sprint brief is pasted, it governs the session's scope.