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
    - Micronaut 5.0 GA'd May 2026 and moved its baseline to Java 25. This project stays on 4.x/21 deliberately — 4.x continues full support on Java 17/21, and there's no reason to chase a one-month-old major release mid-build. Do not suggest upgrading to 5.0.
- PostgreSQL 16 (Docker Compose) · Micronaut Data **JPA** · Flyway (hbm2ddl off, always)
    - **JPA wiring is intentionally NOT in the build yet.** Micronaut eagerly builds a Hibernate `SessionFactory` at startup, which fails hard (`Entities not found for JPA configuration`) if zero `@Entity` classes exist — and none do until Sprint 0.2. The Hibernate dependencies (`micronaut-data-hibernate-jpa`, `micronaut-hibernate-jpa`, the `micronaut-data-processor` annotation processor) are commented out in `build.gradle.kts`, marked `// re-add Sprint 0.2`. Datasource + Flyway are active and don't need Hibernate. Don't suggest adding a placeholder entity to work around this — wait for real entities in 0.2.
- Config format is **`application.properties`**, not YAML — this is Micronaut 4's default (it dropped the bundled SnakeYAML dependency). Don't convert to `.yml` without good reason; it just adds a dependency for no benefit here.
- Lombok · Bean Validation on DTOs
- LocalStack for S3 (added Sprint 1.2) — same SDK, endpoint override in local profile
- Profiles: `local` is the dev default (stub AI, local DB, LocalStack). Real AI only when explicitly switched.
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
Principles: questions are tagged with concepts; spec sections map to concepts; one question serves any board/spec/level sharing the concept. Spec revisions = new specification version + new mappings; questions untouched. Synoptic questions = multiple concepts. Prefer **explicit join entities** over @ManyToMany for the junctions. **Students only ever see status=approved.**

## 7. API surface

```
GET  /health
GET  /api/v1/concepts
GET  /api/v1/concepts/{id}
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

## 9. Testing (first-class, written alongside features)

- **Unit:** JUnit 5 + Mockito. Service layer, all deps mocked, no context, fast. Naming `*Test`.
- **Integration:** `@MicronautTest` + **Testcontainers real PostgreSQL — never H2**. Naming `*IT`. Real migrations, real SQL, real HTTP.
- Gradle split:
```kotlin
tasks.test { useJUnitPlatform(); exclude("**/*IT*") }
tasks.register<Test>("integrationTest") { useJUnitPlatform(); include("**/*IT*") }
```
- StubAIService is the test default. Every service method: happy path + meaningful edge cases. Tests must be independent and readable.

## 10. Hard rules

- Constructor injection only — never field `@Inject`.
- No business logic in controllers or repositories.
- No AI calls outside the `ai/` package / AIService interface.
- Flyway only — never hbm2ddl create/update.
- No secrets in code, config, or commits.
- **No copyrighted material committed — ever.** Real past papers may be used privately for local testing; all committed fixtures/seed content are self-written. Seed concept lists may mirror the published spec's topic structure (facts), not its prose.
- Commit small and often, meaningful messages — the public history is CV evidence.
- Don't build Phase 4+ infrastructure early (SQS/processor/Lambda/Terraform).

## 11. Plan & current position

Sprint sequence: **0.1** skeleton+CI → **0.2** schema+read API+seed script → **0.3** attempts → **1.1** extractor (separate repo) → **1.2** upload+pipeline (stub AI) → **1.3** review API → **2.1** student UI → **2.2** superuser UI → **2.3** demo hardening = **interview-ready cut** → (bonus) 3 AI marking → 4 service extraction → 5 AWS → 6 accounts/progress/generation.

> **Current sprint: 0.1 — Skeleton + CI** *(update this line as sprints complete)*
>
> Sprint 0.1 DoD: build green local+CI · compose Postgres healthy · /health UP · Flyway baseline runs · 1 unit test green · 1 Testcontainers IT green (timebox WSL2/Docker issues ~1h; defer to 0.2 with TODO if stuck) · README with badge + run instructions.

Full sprint briefs live in PRACTIQ_MASTER.md (the planning doc, kept outside the repo). If a sprint brief is pasted, it governs the session's scope.
