# practiq-api

![Main](https://github.com/jamiewmeldrum/practiq-api/actions/workflows/main-push.yml/badge.svg)

Adaptive learning/practice platform API (Java 21 · Micronaut 4.10 · PostgreSQL 16).

## Local workflow (from cold)

The full loop from nothing running to a seeded, queryable app:

```bash
docker compose up -d                       # 1. Postgres 16 (:5432) + LocalStack S3 (:4566)
./gradlew run                              # 2. start the app — Flyway applies migrations at boot
docker exec -i practiq-api-postgres-1 \
  psql -U practiq -d practiq < src/main/resources/db/seed_local.sql   # 3. load sample data
```

Then hit the app:

```bash
curl http://localhost:8080/health
curl http://localhost:8080/api/v1/concepts
```

**Order matters between steps 2 and 3.** Flyway runs the migrations at *application
startup*, not during the build — so the tables don't exist until the app has booted at
least once against the Compose DB. Seeding before the first `./gradlew run` fails with
"relation does not exist". After that first boot the schema persists in the Compose
volume, so on later loops you can seed anytime the container is up.

`./gradlew run` holds the terminal (it runs the server in the foreground), so run the
seed and `curl` commands from a second terminal — or start the app from IntelliJ instead
(see [Running/debugging from IntelliJ](#runningdebugging-from-intellij), and remember the
`MICRONAUT_ENVIRONMENTS=local` it requires). Stop the app with `Ctrl+C`; stop Postgres
with `docker compose down` (add `-v` to also wipe the data + schema for a clean replay).

The sections below expand each step: [Running locally](#running-locally) for the app,
[Local development data](#local-development-data) for seeding, and
[Querying the local database](#querying-the-local-database) for inspecting it.

## Running locally

Start the Compose Postgres, then run the app:

```bash
docker compose up -d            # Postgres 16 (:5432) + LocalStack S3 (:4566)
./gradlew run                   # serves http://localhost:8080
```

`./gradlew run` defaults to the `local` environment (`application-local.yml`), so
the app connects to the Compose database. Verify it's up:

```bash
curl http://localhost:8080/health
```

> The `local` default is wired into the `run` task in `build.gradle.kts`. Without it, the
> base config has no datasource URL and Micronaut Test Resources would start a throwaway
> Postgres container instead of using your Compose DB.

The `run` task also supplies `PRACTIQ_ADMIN_KEY=local-admin-key`, so admin routes expect
`X-Admin-Key: local-admin-key` on a locally started app:

```bash
curl -H "X-Admin-Key: local-admin-key" http://localhost:8080/api/v1/admin/documents
```

### Running/debugging from IntelliJ

Running the application's main class directly in IntelliJ (e.g. to attach the debugger)
bypasses the Gradle `run` task, so the `local` environment is **not** activated
automatically. Add an environment variable to the run configuration:

```
MICRONAUT_ENVIRONMENTS=local
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
PRACTIQ_ADMIN_KEY=local-admin-key
```

Without `MICRONAUT_ENVIRONMENTS`, the app starts with no datasource URL and Test Resources
spins up a throwaway Postgres container instead of connecting to your Compose DB. The two
AWS variables are the LocalStack credentials the `run` task supplies (see
[LocalStack and S3](#localstack-and-s3)); without them any S3 call fails to resolve
credentials.

`PRACTIQ_ADMIN_KEY` fails differently from the other three: it is the only one the app
refuses to start without. `application.yml` binds `practiq.admin-key` straight from it with
no fallback, and `AdminKeyValidator` throws from its constructor if the value is missing or
blank — so the context never comes up, and you find out at startup rather than on the first
admin request. That is deliberate: a stand-in credential that silently defaulted to
something would be worse than no credential at all. The value itself is a throwaway and is
never committed to configuration — it lives in the `run` task and in your IDE run
configuration, which is also how a deployed environment will supply the real one.

## LocalStack and S3

`docker compose up -d` starts LocalStack alongside Postgres, with `SERVICES=s3` and the
gateway on `localhost:4566`. `infra/localstack/localstack-setup.sh` is mounted into
`/etc/localstack/init/ready.d/` and runs on every container start, creating the `documents`
bucket. Nothing is persisted between runs, so each `compose up` gives you an empty bucket —
which is what you want locally, and means a `compose down` is the reset button.

Prove it works by hand:

```bash
docker exec localstack awslocal s3 ls                    # documents
docker exec localstack sh -c \
  'echo "hello practiq" > /tmp/probe.txt && awslocal s3 cp /tmp/probe.txt s3://documents/probe.txt'
docker exec localstack awslocal s3 ls s3://documents     # probe.txt, 14 bytes
docker exec localstack awslocal s3 cp s3://documents/probe.txt -   # hello practiq
docker exec localstack awslocal s3 rm s3://documents/probe.txt
```

The app reaches it through `aws.services.s3.endpoint-override` in `application-local.yml`,
pointed at `s3.localhost.localstack.cloud:4566` rather than `localhost:4566`. S3 puts the
bucket in the hostname, so a plain `localhost` endpoint produces `documents.localhost`,
which doesn't resolve; `*.localhost.localstack.cloud` is a public DNS wildcard onto
127.0.0.1, so bucket subdomains work and local URLs take the same shape as real S3. It does
mean this needs working public DNS.

Credentials come from `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`, not from
`application-local.yml` — the `run` and `Test` tasks set them in `build.gradle.kts`.
Deployed AWS resolves credentials ambiently from an IAM role, so supplying them the same way
locally keeps one resolution path everywhere. The values are arbitrary; LocalStack never
checks the signature.

## Local development data

`src/main/resources/db/seed_local.sql` holds sample DBRows (concepts, and more data
types as they're added) for manually inspecting the app and exercising endpoints
like `GET /api/v1/concepts`.

It is **not** a Flyway migration — it sits outside `db/migration/`, so Flyway never
runs it. Seeding is deliberately separate from schema: real content will be
AI-generated and human-reviewed through the ingestion pipeline, so it doesn't belong
baked into migrations. Tests generate their own data and don't use this file.

Load it (Compose Postgres must be up):

```bash
docker exec -i practiq-api-postgres-1 psql -U practiq -d practiq < src/main/resources/db/seed_local.sql
```

The script is **idempotent** — every insert uses `ON CONFLICT DO NOTHING`, so you can
run it as often as you like (after migrations, after a rebuild, whenever) without
duplicate-key errors. Re-running picks up newly added DBRows and leaves existing ones
untouched. New data types get their own section in the same file following the same
pattern, so loading everything stays a single command.

If you want a clean slate — e.g. to drop DBRows that are no longer in the file — reset
first, then reload:

```bash
docker exec -i practiq-api-postgres-1 psql -U practiq -d practiq -c 'TRUNCATE concept CASCADE;'
```

## Querying the local database

To inspect the Compose Postgres directly — checking what a migration produced, eyeballing
seed data, confirming the `status=approved` filter has DBRows to filter — run `psql` inside
the container. No local Postgres client needed; the image already ships one.

One-off query with `-c` (runs, prints, exits):

```bash
docker exec -it practiq-api-postgres-1 psql -U practiq -d practiq \
  -c "select status, count(*) from question group by status;"
```

Interactive shell for a poking-around session:

```bash
docker exec -it practiq-api-postgres-1 psql -U practiq -d practiq
```

Inside the shell, the usual psql meta-commands help: `\dt` lists tables, `\d question`
describes one, `\x` toggles expanded (row-per-line) output for wide DBRows, `\q` quits.
`select * from flyway_schema_history;` shows which migrations have been applied.

The `-U practiq -d practiq` flags are the local user and database from
`docker-compose.yml`; `practiq-api-postgres-1` is the container name Compose generates
(`docker ps` if yours differs). Use `-it` for the interactive shell and for `-c` queries
you want to read in the terminal; `-i` alone is enough when piping SQL in from a file, as
the seed-load command above does.

## Logging

Logging runs through **SLF4J** (the API called in code) backed by **Logback** (the
implementation). Both ship with Micronaut — nothing to add. Two concerns live in two
places:

- **Format and destination** — `src/main/resources/logback.xml`: one console appender,
  `root level="info"`. This is where the line pattern and appenders are defined.
- **Levels** — set per environment in `application*.yml` under the
  `logger.levels` map. Micronaut applies these to Logback at startup, so the
  local dev loop can be noisy while tests stay quiet, without touching the XML.

### Adding a logger in code

With Lombok, annotate the class with `@Slf4j` and use the generated `log` field:

```java

@Slf4j
@Singleton
public class ConceptService {
    public Concept findById(UUID id) {
        log.debug("looking up concept {}", id);   // parameterised — never string concat
        ...
    }
}
```

The logger's name is the fully-qualified class name
(`com.practiq.service.ConceptService`), which is what the level config below targets.

### Changing levels

A level switches on that severity **and everything above it**:
`TRACE < DEBUG < INFO < WARN < ERROR`. Setting a package to `DEBUG` shows
DEBUG/INFO/WARN/ERROR from it.

Global floor — change the root in `logback.xml`:

```xml

<root level="info">          <!-- debug for a firehose, warn for near-silence -->
```

Per-package, per-environment — add to the relevant config file. To see `DEBUG`
from your own code **in the local dev loop only**, add to `application-local.yml`:

```yaml
logger:
  levels:
    com.practiq: DEBUG
```

Tests and every other environment inherit the root `info`, so this doesn't make test
output noisy. Scope it tighter when chasing one thing —
a `com.practiq.service.ConceptService: TRACE` entry targets a single class.

### Switching on framework logging

The same `logger.levels` map turns on framework internals while diagnosing. Add them
to `application-local.yml` while you need them, then remove:

```yaml
logger:
  levels:
    org.hibernate.SQL: DEBUG               # generated SQL statements
    org.hibernate.orm.jdbc.bind: TRACE     # bound parameter values
    io.micronaut.http.client: DEBUG        # outbound HTTP (extractor client, later)
    org.flywaydb: DEBUG                    # migration execution
```

Inbound request access logging is separate (handled by Netty, not a logger level) —
enable with `micronaut.server.netty.access-logger.enabled=true` when needed.

Levels can also be set via environment variable for a one-off run without editing files
(dots become underscores, uppercased):

```bash
LOGGER_LEVELS_COM_PRACTIQ=DEBUG ./gradlew run
```

## API

All routes are versioned under `/api/v1`. Responses are JSON with nulls included
(`micronaut.serde.serialization.inclusion=always`), so a field's absence is a contract
change, not a data artefact.

| Endpoint                    | Returns                                                                  |
|-----------------------------|--------------------------------------------------------------------------|
| `GET /health`               | liveness                                                                 |
| `GET /api/v1/concepts`      | all concepts, `created_at` ascending — bare array (deliberately unpaged) |
| `GET /api/v1/concepts/{id}` | one concept, or the 404 envelope                                         |
| `GET /api/v1/questions`     | paginated, filterable question catalogue — see below                     |
| `POST /api/v1/admin/documents` | registers a document and returns a presigned upload URL — see below   |

### Validation applied to every route

| Sent | Answer |
|---|---|
| a path id below `1` | **422** `id: must be greater than or equal to 1` — an id that can never name a row is a bad value, not an absent one |
| a path id that is not a number | **400** `id: invalid value` |
| `page`/`size` that are not numbers | **400** — the same conversion failure a bad filter gives |
| `page` below `0`, `size` below `1` | **422**, naming the rule |
| `size` above `micronaut.data.pageable.max-page-size` | **422** `size: must not be greater than 50` — stated back rather than silently clamped, because a client handed 50 rows after asking for 500 cannot tell a ceiling from the end of the data |
| `sort` on any endpoint | **422** `sort: is not supported` — the query runners impose a total order so pages cannot straddle, so no client sort could be honoured |
| a blank or over-64-character `X-Session-Token` | **422**, before any lookup runs |

Paging past the end is **not** an error: the envelope echoes the requested position with no rows, which is
what lets a client walk pages without knowing the total in advance.

### `GET /api/v1/questions`

Serves the **student catalogue**: only `APPROVED` questions that are linked to at least
one concept (an unlinked question is unprocessed and never student-visible). Query params:

- `types` — CSV of `SHORT_ANSWER|EXTENDED|MCQ`
- `difficulties` — CSV of numeric codes `1..5` (`1(TRIVIAL)` … `5(VERY_HARD)`)
- `conceptId` — questions linked to that concept
- `page` / `size` — zero-indexed page and page size (default 10, capped at 50)

Filters only ever *narrow* the result; status is not a parameter. Ordering is a
server-enforced total order (`created_at`, then `id` as tiebreak) so pages are stable
and rows can't repeat or vanish across a page boundary.

Paged responses use an envelope; unpaged collections (concepts) deliberately don't:

```json
{
  "content": [
    {
      "id": 7,
      "body": "State Newton's first law.",
      "difficulty": {
        "value": 3,
        "code": "MEDIUM"
      },
      "type": "EXTENDED",
      "createdAt": "2026-06-29T10:15:30Z",
      "linkedConceptIds": [
        10,
        11
      ]
    }
  ],
  "page": 0,
  "size": 10,
  "totalCount": 1
}
```

`difficulty` serialises as `{value, code}` (whole object `null` when unrated); nominal
enums (`type`) serialise as their bare code. Provenance fields (`source`, `status`,
`source_spec`) are deliberately not exposed to students.

### `POST /api/v1/admin/documents`

Admin-gated by the static `X-Admin-Key` header (the stopgap until real auth). Registers a
document at `AWAITING_UPLOAD` with a server-minted UUID `s3_key`, and returns a presigned
`PUT` URL the caller uploads to directly — the API never carries the bytes.

```json
{ "filename": "aqa-2007-paper1.pdf", "contentType": "application/pdf", "contentLength": 2048,
  "sourceSpec": "AQA 2007" }
```

returns `201` with `{"id": 7, "url": "https://…", "expiresAt": "2026-08-12T18:10:00Z"}`.

The product limits live in one place, `service/document/DocumentUploadRules`: **25MB** maximum
declared size and a **10 minute** URL expiry. Both are bound into the signature, so S3 rejects a
body of a different length and the URL simply stops working — the size cap holds before any
bytes (or bill) land. `contentType` is validated against an allow-list *and* against the type
derived from the filename extension; the derived one is what gets signed, so a client's declared
value is checked, never trusted.

The upload must send exactly the signed `Content-Type` and `Content-Length`. They are inside
`X-Amz-SignedHeaders`, so any deviation — including a `; charset=…` suffix a client library adds
for you — invalidates the signature against real S3.

## Error handling

All errors aim to return one envelope: `{"error": "...", "status": <code>}` (see
`dto/ErrorResponse`). Each case is an `ExceptionHandler` in `exception/`; Micronaut
routes a thrown exception to the **most specific** handler registered for its type.

Current coverage:

- **400** — `ConversionErrorExceptionHandler` (binding/conversion failures; replaces
  Micronaut's default). For enum-typed params it enumerates the legal values, e.g.
  `?types=BAD` → `"types: must be one of SHORT_ANSWER, EXTENDED, MCQ"` and
  `?difficulties=9` → `"difficulties: must be one of 1(TRIVIAL), 2(EASY), …"`.
- **401** — `UnauthorizedExceptionHandler` (admin routes: missing, blank or wrong
  `X-Admin-Key`). The header binds as `@Nullable` so an absent one reaches the validator rather
  than failing to bind, and all three cases return a byte-identical body — a caller cannot tell
  a missing key from a wrong one.
- **404** — `NotFoundExceptionHandler` (unmatched route / missing resource).
- **413** — two handlers, deliberately distinguishable. `ContentLengthExceededHandler` answers
  when Micronaut rejects an oversized HTTP body; `ContentTooLargeExceptionHandler` answers our
  own product rules, e.g. `"contentLength: must not be greater than 26214400"`.
- **422** — two sources, one format. `ConstraintViolationExceptionHandler` (bean-validation
  failures on an otherwise-parseable request, e.g. `@UniqueElements` duplicates), one message
  per violation, sorted and joined; and `EntityValidationExceptionHandler` for rules expressed
  in code rather than annotations. Both emit `field: reason` (`"filename: must not be blank"`),
  so a client sees one grammar regardless of which layer rejected the request. The format is
  built once in `dto/mapper/ErrorResponseMapper`, not at each throw site.
- **500** — `GenericExceptionHandler` catches any otherwise-unhandled `Exception` as a
  last-resort safety net: consistent envelope, logged at `ERROR`, no internals leaked.
  (An escaping `OptimisticLockException` currently lands here too — pinned by a CT; the
  first write endpoint should introduce a dedicated `409 Conflict` handler.)

One deliberate asymmetry, pinned by CTs: **`Pageable` params never 400.** Micronaut's
binder is lenient — `?page=abc` or `?size=0` silently fall back to the defaults, and
`?size=500` is capped at the configured maximum (50) — whereas filter params
(`?conceptId=abc`) fail loudly with the 400 envelope. Making paging params strict would
need a custom `Pageable` binder; not worth the machinery yet.

## Formatting

Formatting is owned by [Spotless](https://github.com/diffplug/spotless) using
[palantir-java-format](https://github.com/palantir/palantir-java-format). There is no house style to
learn and nothing to argue about in review — the formatter decides.

```bash
./gradlew spotlessApply     # fix formatting
./gradlew spotlessCheck     # report violations (runs as part of `check` / `build`)
```

### Enable the pre-commit hook

**Do this once per clone:**

```bash
git config core.hooksPath .githooks
```

Git never clones hook configuration — only the scripts in `.githooks/` travel with the repo, and the
setting that points git at them lives in `.git/config`, which doesn't. So a fresh clone has the hook
sitting there inert until the command above is run.

The hook runs `spotlessApply` before each commit. If it changed nothing, the commit proceeds. If it
did change something, **the commit is aborted** and the reformatting is left unstaged in your working
tree, with the affected files listed. Review it, stage it, commit again.

It deliberately never stages anything for you. An auto-staging hook puts changes into a commit that
nobody looked at, and if you had staged a file partially (`git add -p`), re-adding the whole file
after reformatting would silently drag in the hunks you meant to leave out.

Bypass with `git commit --no-verify` when you need to.

The hook is a convenience, not a gate — it can be skipped, unset, or never enabled. **CI is the
enforcement**: the pull request pipeline runs `spotlessCheck` as its own gate, so badly formatted
code fails regardless of anyone's local setup.

## Layering

Three layers and two leaf tiers. Imports run one way only, and that direction is the one thing here a
compiler can check.

```
web/          inbound adapter — controllers, request/response DTOs, mappers, exception handlers, binders
service/      business logic — models, commands, policies, accessors, mappers
persistence/  outbound adapter — entities, projections, repositories, query runners, specification factories
storage/      outbound adapter — S3
foundation/   shared vocabulary — enums, exception types, pure helpers
```

`web → service → persistence`. `persistence` and `storage` import nothing of ours but `foundation`;
`foundation` imports nothing of ours at all. A grep for `com.practiq.service` under `persistence/` should
return nothing, and the same for `com.practiq.web` under `service/`.

**Nothing is called `domain`.** The name was tried and dropped: logic lives in services, so there is no rich
domain model for it to describe. Each layer owns the types it hands out, and the service's are records under
`service/<feature>/dto/`.

**The service never exposes a persistence type.** `ConceptEntity` becomes `Concept`, `QuestionEntity` becomes
`Question`, and so on — the bare noun belongs to the type that crosses a boundary, the `Entity` suffix to the
one Hibernate manages. Entities and projections may reach the service; they may not reach the web layer. That
boundary is held by the service's return signatures, not by naming.

**Service models are not entity shadows.** They carry what is safe to expose beyond the service — including
things the wire never sees. `Question` carries `version` and `status`; `QuestionResponse` carries neither.
`QuestionAttempt` carries the session token; the response drops it rather than echoing a caller-supplied,
credential-shaped value back into bodies and logs.

### Where a query gets built

Persistence answers the query it is handed. It does not know who is asking or why.

```
QuestionService → QuestionAccessor → QuestionQueryRunner → QuestionRepository
                       ↑ holds the policy
```

- **`StudentQuestionQueryPolicy`** (service) decides what an audience may see: `APPROVED`, concept-linked.
- **`QuestionAccessor`** (service) is the only place that policy is applied. It is built per audience by
  `QuestionAccessorFactory` and injected by qualifier — `@Named("student")` — so the choice is made once in a
  field declaration and a holder of the student accessor cannot make an admin read.
- **`QuestionQueryRunner`** (persistence) executes: the specification, the stable `(created_at, id)` order,
  and the two-query concept-link stitch. It has no policy and no idea one exists.

The accessor also owns the one guarantee no other tier can make — that an id query matched at most one row.
The runner cannot know that; the accessor can, because it built the query.

**Accessors exist only where there is a query to build** — a policy to apply or a cardinality expectation to
enforce. Concept, mark scheme and attempts have neither, so their services use named repository finders
directly. There is exactly one accessor in the codebase, and that is deliberate.

## Testing

Four tiers. The guiding rule: **put each test where it can actually observe the behaviour it claims to
verify.** Mocking everything around a thin layer just tests a tautology.

| Tier        | Suffix  | Wires                                                        | Boundary                       | Task              |
|-------------|---------|--------------------------------------------------------------|--------------------------------|-------------------|
| Unit        | `*Test` | one class, no context                                        | all deps mocked (Mockito)      | `test`            |
| Component   | `*CT`   | real web layer (routing, binding, validation, serialization) | repository mocked, no DB       | `test`            |
| Integration | `*IT`   | full stack                                                   | real Postgres + LocalStack S3  | `integrationTest` |
| Performance | `*PT`   | full stack                                                   | real Postgres (Testcontainers) | `performanceTest` |

```bash
./gradlew test                  # unit + component (*CT) — the every-change loop
./gradlew integrationTest       # integration (*IT) — real Postgres, pre-merge / CI
./gradlew performanceTest       # performance (*PT) — real Postgres, per-request query counts
./gradlew build                 # runs everything
```

CI does not use `build`. It runs each gate as a separate named step so one failure doesn't hide the
rest — see `.github/workflows/pull-request.yml`.

`main` is protected by a repository ruleset requiring the `Pull request checks` job to pass, plus
linear history and no force-pushes or deletion. A copy lives at
`.github/rulesets/main-protection.json` **for reference only** — GitHub stores the live rule, and
nothing in this repo applies or verifies it. Re-import it from the ruleset page if it ever needs
restoring.

### What each tier answers

How a test is wired tells you where it plugs in. Only the **question** tells you whether it's worth
having — so that's what the name encodes:

| Naming                                      | Question it answers                                    |
|---------------------------------------------|--------------------------------------------------------|
| `*Test`                                     | Does this logic do what it should?                     |
| `*CT`                                       | Does the web layer bind, serialise and map correctly?  |
| `*ControllerIT`                             | Does the definition of done actually hold, end to end? |
| `*RepositoryIT`                             | Do I understand the method I'm calling?                |
| `*DatabaseIT`                               | Does the migration say what I think it says?           |
| `*PT`                                       | Is the query plan the shape I think it is?             |

### Tests never import an application constant

A test that asserts against `MAX_CONTENT_LENGTH` imported from production moves in lockstep with it:
change the value and the test still passes, having proved nothing. Product values are therefore
restated as **test-side** constants — the block at the top of `TestData`, named for what they pin
(`DOCUMENT_FILENAME_MAX_LENGTH`, `QUESTION_ATTEMPT_SESSION_TOKEN_MAX_LENGTH`, and so on) — so the two are
independent statements of the same rule and a change on either side is a failure you have to look at. That
is the pin.

The exception is a static import of the method under test (`ConceptResponseMapperTest` importing
`toConceptResponse`): that is the subject of the test, not a value it is checking against.

**Three IT flavours, one tier.** `*ControllerIT`, `*RepositoryIT` and `*DatabaseIT` share a tier because
they share a *mechanism* — they need a container and run in `integrationTest`. They differ in the question,
which the name carries. `IT` here honestly means "needs Docker", which is true of all three; "integration"
is a conceptual promise the suffix never made.

No tier substitutes for another. A repository test proves you understand your tools; it does not prove the
app calls them. Swap `findOne(spec)` for a hand-rolled query and the repository test still passes — it now
tests a method production abandoned, and it goes stale silently. Only the controller IT's meaning survives
that refactor.

### Proving the right collaborator was wired in

`QuestionService` is bound to a policy-bound accessor by qualifier alone. Get that wrong and every read
silently widens — students receive `PENDING` questions, with a 200 and a plausible body.

No unit test can catch it: a unit test constructs the accessor itself, so it proves the accessor works, not
that the right one arrived. `QuestionServiceAccessorCT` starts the real context, mocks only the repository,
and resolves the specification the service caused through `utils.CriteriaProbe` — asserting it carries
`status = APPROVED` and the concept-link `EXISTS`.

That test is worth having only if it fails when the wiring breaks, so it was checked against a policy with
its restrictions removed. Both cases failed.

**`CriteriaProbe` proves which criteria calls were made, never which rows come back.** That is why the
specification factories are proven in `QuestionRepositoryIT` and `QuestionAttemptRepositoryIT` instead: the
concept filter has to be a correlated `EXISTS` rather than a join, because a join multiplies question rows
per link and corrupts `totalCount` — and a probe sees a subquery either way. Only real rows can tell those
apart.

### Why there's a performance tier

`*PT` asserts **how** the data was fetched, not **what** came back. Each endpoint gets a happy-path pin on
the number of JDBC statements a single request fires; row-scaling endpoints also assert the count doesn't
grow with the number of rows.

That second assertion catches an N+1. The first one exists for a subtler bug. Reintroduce a fetch-join on a
paginated to-many and Hibernate quietly stops pushing `LIMIT` to SQL — it fetches the whole result set and
pages it in memory. Every correctness test still passes, because the rows returned are *correct*; the
invariance check also passes, because the statement count stays constant. The only thing that moves is the
absolute number of statements. Tests that assert what came back structurally cannot see this. That's the
whole reason the tier exists.

The counts are deliberate magic numbers. Update the pin consciously when you legitimately add a query —
the friction is the feature. They are not annotated with what they are made of: such a comment restates the
code, and the first refactor leaves a correct number with a false explanation.

### Calibration

This is a practice project, and some of this is more than a project this size needs. That's deliberate.
How much test weight is right is a function of team preference and how much the thing costs when it breaks;
the point here was to build the designs out far enough to have an opinion about them. Too much is sometimes
how you find out what enough looks like.

> **Pipeline note:** `*PT` runs as part of the test step on every pull request. Under a git-flow
> dev/main split it may be cleaner to move it to a later stage rather than paying the container cost
> on every push. Easily reversed.

**Writing a component test.** Micronaut has no `@WebMvcTest`-style slice annotation, so a
few things have to be arranged by hand to test the web layer without a database. The pattern
lives in `ConceptControllerCT` + `src/test/resources/application-ctslice.yml`:

```java

@ComponentTest                                  // bundles @MicronautTest(transactional = false, environments = "ctslice")
class ConceptControllerCT {

    @Inject
    EmbeddedServer embeddedServer;       // for RestAssured.port
    @Inject
    ConceptRepository conceptRepository; // the mock, for stubbing

    @MockBean(ConceptRepository.class)
    ConceptRepository conceptRepository() {
        return mock(ConceptRepository.class);
    }

    // when(conceptRepository.listOrderByCreatedAtAsc())...; then GET /api/v1/concepts over
    // real HTTP (RestAssured) and assert on the JSON body, not by deserializing into Concept.
}
```

Why each piece is needed:

- **`transactional = false`** — `@MicronautTest` otherwise wraps each test in a rollback
  transaction, and *beginning* that transaction opens a JDBC connection (even though the
  repository is mocked). This is the maintainer-recommended fix.
- **`environments = "ctslice"`** — the `ctslice` properties file supplies a no-op datasource
  and, crucially, `test-resources.containers.postgres.enabled=false` so Test Resources does
  **not** start a Postgres container. It also stops Hikari connecting eagerly and Hibernate
  probing JDBC metadata at boot, so the context starts with no database at all.
- **Assert on the wire format, not the entity** — `Concept` is `@Getter`-only (no setters),
  so deserializing the response back into `Concept` leaves fields null. Retrieve into a
  `Map`/`String` and assert the JSON, which is what the endpoint actually returns.

Test bodies are scaffolded with `fail("not yet implemented")` so unwritten coverage shows
red rather than a misleading green.

**Provoking framework-level failures (test-scoped controller).** A global exception handler
(e.g. the `UnsatisfiedRouteException` → 400 envelope handler) is a cross-cutting concern, not
the property of any one endpoint. Triggering it through a real app controller couples the
error-handling test to that controller's unrelated behaviour. Instead `ErrorHandlingCT` owns a
`ErrorHandlingTestController` whose only job is to fail on demand — a missing required
`@Header`, a missing `@QueryValue`, a thrown `RuntimeException`, etc. It is scoped to that one
test with the Micronaut `spec.name` idiom so it never pollutes other contexts:

```java
@Requires(property = "spec.name", value = "ErrorHandlingCT")   // on the test controller
@Controller("/test/errors") ...

@Property(name = "spec.name", value = "ErrorHandlingCT")        // on the test class
class ErrorHandlingCT { ...
}
```

Only the context whose `spec.name` matches loads the controller. Handling the abstract
`UnsatisfiedRouteException` (not the header-specific subclass) means one handler serves every
missing-binding flavour; the CT proves that breadth by exercising two siblings (header + query
value) rather than every subtype.

**Writing an integration test.** `*IT` tests drive the full stack over real HTTP against a
real Postgres, and arrange their data with **raw SQL**, not the repositories — so a failure
points at the code under test, not at the persistence code used to set it up (and we avoid
adding production methods like `deleteAll` purely for tests). The helper is
`utils.data.TestDatabase` (`insert(table, Map<col,value>)` / `update(table, id, col,
value)` / `clear(table)`); the pattern lives in `ConceptControllerIT`:

```java

@IntegrationTest                                // bundles @MicronautTest(transactional = false)
class ConceptControllerIT {

    @Inject
    TestDatabase testDatabase;
    @Inject
    EmbeddedServer embeddedServer;

    @BeforeEach
    void setUp() {
        testDatabase.clear("concept");                  // global TRUNCATE ... RESTART IDENTITY
        RestAssured.port = embeddedServer.getPort();
    }
    // insert(...) fixtures, then GET over HTTP and assert order-agnostically.
}
```

Getting `TestDatabase` to hand out genuinely independent connections took three things that
only make sense together — the injected `DataSource` is Micronaut's *connection-managed*
wrapper, not a raw pool:

- **`transactional = false`** — `@MicronautTest` otherwise wraps each test in a managed
  transaction, and `getConnection()` then returns *that transaction's* connection. Inserts
  would join the test transaction: uncommitted, invisible to the HTTP request (which runs on
  another thread), and rolled back at the end. Turning it off removes the entanglement.
- **`DataSourceResolver.resolve(dataSource)`** (in `TestDatabase`) — with no managed
  transaction, the wrapped `DataSource` has no connection scope to draw from and throws
  `NoConnectionException`. Resolving to the underlying Hikari pool yields plain connections
  that need no ambient scope. (It's still the pool — just without the management layer.)
- **`connection.setAutoCommit(true)`** — the resolved pool still hands out `autoCommit=false`
  connections (Hibernate's default), so an insert that doesn't commit is rolled back when the
  connection returns to the pool. Autocommit makes "committed before the request runs" true
  unconditionally.

Because `id` and `created_at` are generated by the database, the *shape* tests assert their
shape (`everyItem(greaterThan(0))`, an ISO-8601 pattern) rather than fixed values, and look
DBRows up by name (`find { it.name == … }`) instead of by position — see [Integration tests share
one database](#integration-tests-share-one-database--keep-them-sequential) for why position
isn't assumed there.

The list endpoint itself *does* have an ordering contract — `GET /api/v1/concepts` returns
`created_at asc`. That contract is pinned separately by `getConceptsReturnsInCreatedAtOrder`,
which re-stamps one row's `created_at` (via `TestDatabase.update`) to prove the order isn't
incidental. The shape tests staying order-agnostic is a deliberate split: they verify *what*
comes back, the ordering test verifies *in what order*.

### `test` still needs Docker — a deliberate compromise

Component tests no longer start a Postgres container (the `ctslice` env disables it), but
`./gradlew test` **still requires Docker**. This is a conscious trade-off, not an oversight,
so the reasoning is recorded here.

**Why it can't be fully removed cheaply.** The Micronaut Gradle plugin attaches the Test
Resources *service* to **every** `Test` task. That service is a separate JVM that connects
to Docker and starts a `ryuk` reaper container as soon as it boots — before any test runs,
regardless of whether a test needs a resource. The plugin's `enabled` flag is *project-wide*
([there is no per-task toggle](https://github.com/micronaut-projects/micronaut-test-resources/issues/766)),
so you can't disable it for `test` while keeping it for `integrationTest` within one module.
Our `ctslice` trick removes the *Postgres* container; it cannot remove the service + ryuk.

**Why we keep Test Resources anyway.** It's the part that *scales*. It provisions containers
declaratively, supports a shared server, and ships modules for the resources we add as we go —
the LocalStack S3 the `*IT` tier uses arrived that way in Sprint 0.3, as one dependency and
two lines of config. Hand-rolling Testcontainers per `*IT` (or per service) to win a
Docker-free `test` would trade a small, well-understood cost for boilerplate that grows with
every new IT class, resource type, and microservice. That's optimising the wrong axis.

**The real fix, when it's worth it.** A genuinely Docker-free fast loop needs Test Resources
*isolated to an integration module* (a separate Gradle module — or a shared convention plugin
once there are multiple services — that applies the plugin, while the unit/component module
does not). A separate source set alone is not enough, because the plugin is project-global.

**Decision: deferred indefinitely.** Not "deferred until X" — there is no problem to solve.
`./gradlew test` pays about a second of ryuk startup and is otherwise unaffected, and a fixed
second is not worth a module split that cuts against the monolith-first stance in `CLAUDE.md` §3.

An earlier version of this section listed triggers to revisit on: several `*IT` classes,
LocalStack arriving, the first service extraction. Two of those have since happened — the tier
is now 18 classes and LocalStack landed in Sprint 0.3 — and the fast loop is no different for
it. They were the wrong signal: they measured how much the test suite had grown, not what it
costs to run. The only thing that would justify revisiting is the everyday `test` loop actually
becoming slow enough to interrupt the change/verify cycle, and if that ever happens, a
one-second container start is unlikely to be why. Long-term backlog, tracked as
`TODO(test-resources)` in `build.gradle.kts`.

What the `ctslice` + `@ComponentTest` machinery buys us today is still real: component tests
do no DB work and express the intended boundary (web layer in, persistence mocked). We keep
it as the design statement, and the module split later turns "no Postgres container" into
"no Docker at all" without changing how the tests are written.

### What the `*IT` tier provisions

`*IT` tests get a real Postgres 16 **and** a LocalStack S3, both started by Micronaut Test
Resources, so Docker must be available for `integrationTest`. Neither comes from
`docker-compose.yml` — Compose is for the dev loop only, and a test run never touches it.

The two are wired differently, which is worth knowing before you debug a connection error:

- **Postgres** is resolved because the base `application.yml` deliberately has no datasource
  URL, so Test Resources fills the gap.
- **LocalStack** is resolved the same way — nothing in the test environment sets
  `aws.services.s3.endpoint-override`, so Test Resources supplies it, pointed at the
  container it started. The provider comes from the `testResourcesService` dependency in
  `build.gradle.kts`; `localstack` in `test-resources.containers.localstack` is that
  provider's own container name, not one we chose.

Both images are pinned in `src/test/resources/application-test.yml`. Unpinned, they track
`latest` and drift out from under CI.

**Presigning does not need any of this.** Computing a presigned URL is a local HMAC over the
request parameters — no call is made to S3 — so `S3DocumentStorageCT` produces a genuinely
signed URL in the Docker-free `test` tier, and asserts what a client actually receives
(`X-Amz-Expires`, `X-Amz-SignedHeaders`). It needs only `aws.region` in
`application-ctslice.yml`; credentials already arrive as env vars on every `Test` task. Worth
knowing the division: the CT evidences that the signature carries the right parameters, the IT
evidences that the URL addresses the right object and a real `PUT` lands. LocalStack does **not**
verify signatures, so the IT cannot evidence signature validity — a URL with a fabricated
`X-Amz-Signature` uploads successfully against it.

There is **no `ready.d` init script** in the test tier — Test Resources gives you a bare
LocalStack. The `documents` bucket exists in an IT only because `utils/aws/S3TestUtils`
creates it. That is the opposite of the Compose setup, where the init script owns the bucket
and the app assumes it is already there.

### Integration tests share one database — keep them sequential

Test Resources starts **one** Postgres container for the whole build, and every `*IT`
connects to it. Each test resets state itself (truncate + insert its own fixtures in
`@BeforeEach`), and that reset is **global** — it clears the whole table, not just the
calling test's DBRows.

That means `*IT` tests **must run sequentially**, which they do by default: Gradle's
`maxParallelForks` is `1` and JUnit 5 parallel execution is off. **Don't enable either**
for `integrationTest`. If you do, tests race on the shared table — one test's truncate
wipes the DBRows another just inserted and is about to read. And because read endpoints
return the whole table and assertions pin the *exact* set returned (`containsInAnyOrder`),
a concurrent writer's DBRows break the assertion no matter how surgical the cleanup is.
Whole-table read + exact-set assertion is fundamentally incompatible with concurrent
writers on the same table.

If the IT suite ever grows enough that speed matters, the fix is **not** raw parallelism:
either keep DB-touching tests serial with `@ResourceLock` / `@Execution(SAME_THREAD)` and
parallelise only the fast (`*Test`/`*CT`) tiers, or give each worker its own database/schema.
Both are deferred — the IT tier is deliberately kept small (see §9 of `CLAUDE.md`), so
sequential is correct for now.

## Continuous integration

Three workflows, deliberately separate because they answer different questions.

| Workflow | Trigger | Question |
|----------|---------|----------|
| `pull-request.yml` | PR against `main` | Is this change safe to merge? |
| `main-push.yml` | push to `main` | Is `main` healthy, and what needs cleaning up? |
| `codeql.yml` | PR, push to `main`, weekly | Is there a known vulnerability pattern in the code? |

### Everything is a named step

Neither workflow runs `./gradlew build`. Each gate is invoked explicitly:

```
spotlessCheck                              formatting
compileJava compileTestJava                Error Prone (a javac plugin — it has no task of its own)
test integrationTest performanceTest       all three test tiers
shadowJar                                  the artefact
```

`build` would run all of these via `check`, but as one opaque task. Naming them separately means
a failure is attributed to a gate rather than to "the build", and the gates that *can* still run
after a failure do.

### One push reports every failure it can reach

Each gate carries `if: ${{ !cancelled() }}`.

A step's implicit condition is `success()`, which is false once anything has failed — that's why
a normal pipeline skips everything after the first failure. Overriding it with `!cancelled()`
removes the failure check while leaving the failure itself intact, so the remaining gates run and
the job still goes red. Use `!cancelled()` and not `always()`: the latter also runs when you
cancel the run, which makes a stuck pipeline unkillable.

`continue-on-error: true` looks like the same thing and isn't. It rewrites the step's *conclusion*
to success, which hides the failure from the job as well as from the following steps, so the
verdict then has to be reconstructed by hand from each step's `outcome`.

Tests and artefact are the exception — they additionally require
`steps.errorprone.outcome == 'success'`, because there is no binary to test or package if
compilation failed. That is a real dependency, not bookkeeping.

The test step passes `--continue` for the same reason at the Gradle level: without it Gradle stops
at the first failing task, so a unit failure would hide every integration and performance result.

### Formatting and static analysis are not the same gate

`spotlessCheck` reads source text and reports regardless of whether the code compiles. Error Prone
runs *inside* compilation. So a badly formatted file with a compile error reports both in a single
run — which is the whole point of the arrangement above.

Error Prone **errors** fail the build. **Warnings** do not; they are collected on `main` instead
(below).

### Error Prone warnings become one sticky issue

`main-push.yml` tees the compile output and hands it to `.github/scripts/error_prone_report.py`,
which maintains a single GitHub issue titled **Error Prone warnings**:

- warnings present, no issue → create it, labelled `code-quality` and assigned, so the
  notification email actually arrives
- warnings present, issue exists → update the body in place, reopening it if it had been closed
- no warnings → close it

The issue is found by **label**, never by title, and pull requests are filtered out of the result —
the `/issues` endpoint returns PRs as well, so a matching PR title would otherwise be patched
instead. The `code-quality` label must exist before the first run.

Warnings are deliberately not gated on a PR. They are a cleanup backlog, not a blocker, and the
issue is the backlog.

### `shell: bash` is required wherever output is piped

GitHub's implicit default shell is `bash -e` — **without** `pipefail`. So
`./gradlew … 2>&1 | tee errorprone.log` reports `tee`'s exit code, which is always 0, and a failed
compile passes silently. Naming the shell explicitly gets `--noprofile --norc -eo pipefail`:

```yaml
defaults:
  run:
    shell: bash
```

`main-push.yml` needs this because it captures the log. `pull-request.yml` does not pipe and so
does not set it.

### Security scanning

`codeql.yml` runs CodeQL over the Java sources. It uses `build-mode: none`, which extracts from
source rather than compiling — a compiling build would mean a second full Gradle run with all five
annotation processors, roughly doubling CI time for no extra coverage.

It runs on its own workflow rather than as a step in the other two, for three reasons: no existing
workflow has the right trigger set (the weekly cron would otherwise drag the whole test suite along
with it), `security-events: write` stays scoped away from the token that runs Gradle, and a separate
workflow runs concurrently instead of adding to the critical path.

The weekly cron matters because CodeQL's query packs are updated regularly — unchanged code can
still surface a new alert.

Findings block merges via the ruleset's code scanning rule, not by failing the workflow. A run
succeeds whether or not it finds anything; finding something isn't a workflow failure. Current
thresholds are `medium_or_higher` for security severity and `errors_and_warnings` for alert
severity.

#### Known limitation: CodeQL does not understand Micronaut endpoints

**Taint tracking on request-bound parameters does not work on this codebase.** Verified, not
assumed: a deliberate path injection — a request field concatenated into a path and passed to
`Files.delete` — produced zero alerts, with `java/path-injection` confirmed present among the
queries that ran.

The sink is modelled correctly. The *source* is not. CodeQL ships 406 data-extension model files
and 70 framework libraries for Java; none of them cover Micronaut. Without a modelled source,
nothing is attacker-controlled, no taint flows, and the query cannot fire. The same code under
Spring would be flagged immediately.

Support exists upstream but is unmerged: [github/codeql#21387](https://github.com/github/codeql/pull/21387),
open since February 2026. Its five `.model.yml` files could be vendored as a local model pack, but
they only model methods on `HttpRequest` — annotation-bound parameters (`@Body`, `@RequestBean`,
`@QueryValue`) need the QL library changes in the same PR, and those modify core files that a model
pack cannot override. This codebase binds input exclusively through annotations, so vendoring the
data alone would buy nothing.

CodeQL is kept because the source-independent queries still apply — hardcoded credentials, weak
crypto, insecure randomness, XXE. It is not the endpoint-injection scanner it appears to be. Re-test
when #21387 merges.

### Branch protection

`main` is protected by a repository **ruleset** (`main-protection`), not classic branch protection.
It requires the `Pull request checks` job to pass, blocks merges on CodeQL findings above the
thresholds above, and enforces linear history while blocking force-pushes and deletion. Reviews are
not required — a solo repo cannot approve its own PR.

Linear history means the merge button offers squash and rebase only.

The ruleset lives in GitHub, not in this repository; a copy is kept at
`.github/rulesets/main-protection.json` **for reference only**, and nothing here applies or
verifies it. That gap is not theoretical: the required check once pointed at a job name that had
been renamed hours earlier, which made `main` unmergeable with nothing in any diff to show why.

The same applies to the repository security toggles — Dependabot alerts and security updates,
secret scanning, push protection — recorded at `.github/security-settings.json`. Declaring all of
this properly is a Terraform job for `practiq-infrastructure` when that exists.

### Dependabot

`.github/dependabot.yml` configures **version** updates — weekly bumps for `github-actions` and
`gradle`. **Security** updates are a separate mechanism, enabled in repository settings, and fire
whenever a dependency you already use gains a published advisory; they need no entry in that file.

One trap: `ignore` rules in `dependabot.yml` apply to security update PRs as well as version ones.
Silencing a noisy dependency there silently silences its security fixes too. There are no `ignore`
rules today.

## Micronaut 4.10.16 Documentation

- [User Guide](https://docs.micronaut.io/4.10.16/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.10.16/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.10.16/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)

---

- [Micronaut Gradle Plugin documentation](https://micronaut-projects.github.io/micronaut-gradle-plugin/latest/)
- [GraalVM Gradle Plugin documentation](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)
- [Shadow Gradle Plugin](https://gradleup.com/shadow/)

## Feature jdbc-hikari documentation

- [Micronaut Hikari JDBC Connection Pool documentation](https://micronaut-projects.github.io/micronaut-sql/latest/guide/index.html#jdbc)

## Feature flyway documentation

- [Micronaut Flyway Database Migration documentation](https://micronaut-projects.github.io/micronaut-flyway/latest/guide/index.html)


- [https://flywaydb.org/](https://flywaydb.org/)

## Feature management documentation

- [Micronaut Management documentation](https://docs.micronaut.io/latest/guide/index.html#management)

## Feature lombok documentation

- [Micronaut Project Lombok documentation](https://docs.micronaut.io/latest/guide/index.html#lombok)


- [https://projectlombok.org/features/all](https://projectlombok.org/features/all)

## Feature validation documentation

- [Micronaut Validation documentation](https://micronaut-projects.github.io/micronaut-validation/latest/guide/)

## Feature serialization-jackson documentation

- [Micronaut Serialization Jackson Core documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)

## Feature test-resources documentation

- [Micronaut Test Resources documentation](https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/)

## Feature micronaut-aot documentation

- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)


