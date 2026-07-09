# practiq-api

![CI](https://github.com/jamiewmeldrum/practiq-api/actions/workflows/ci.yml/badge.svg)

Adaptive learning/practice platform API (Java 21 · Micronaut 4.10 · PostgreSQL 16).

## Local workflow (from cold)

The full loop from nothing running to a seeded, queryable app:

```bash
docker compose up -d                       # 1. Postgres 16 on localhost:5432
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
docker compose up -d            # Postgres 16 on localhost:5432
./gradlew run                   # serves http://localhost:8080
```

`./gradlew run` defaults to the `local` environment (`application-local.properties`), so
the app connects to the Compose database. Verify it's up:

```bash
curl http://localhost:8080/health
```

> The `local` default is wired into the `run` task in `build.gradle.kts`. Without it, the
> base config has no datasource URL and Micronaut Test Resources would start a throwaway
> Postgres container instead of using your Compose DB.

### Running/debugging from IntelliJ

Running the application's main class directly in IntelliJ (e.g. to attach the debugger)
bypasses the Gradle `run` task, so the `local` environment is **not** activated
automatically. Add an environment variable to the run configuration:

```
MICRONAUT_ENVIRONMENTS=local
```

Without it, the app starts with no datasource URL and Test Resources spins up a throwaway
Postgres container instead of connecting to your Compose DB.

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
- **Levels** — set per environment in `application*.properties` via
  `logger.levels.<name>=<level>`. Micronaut applies these to Logback at startup, so the
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

Per-package, per-environment — add to the relevant properties file. To see `DEBUG`
from your own code **in the local dev loop only**, add to `application-local.properties`:

```properties
logger.levels.com.practiq=DEBUG
```

Tests and every other environment inherit the root `info`, so this doesn't make test
output noisy. Scope it tighter when chasing one thing —
`logger.levels.com.practiq.service.ConceptService=TRACE` targets a single class.

### Switching on framework logging

The same `logger.levels.*` keys turn on framework internals while diagnosing. Add them
to `application-local.properties` while you need them, then remove:

```properties
logger.levels.org.hibernate.SQL=DEBUG               # generated SQL statements
logger.levels.org.hibernate.orm.jdbc.bind=TRACE     # bound parameter values
logger.levels.io.micronaut.http.client=DEBUG        # outbound HTTP (extractor client, later)
logger.levels.org.flywaydb=DEBUG                    # migration execution
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

| Endpoint | Returns |
|----------|---------|
| `GET /health` | liveness |
| `GET /api/v1/concepts` | all concepts, `created_at` ascending — bare array (deliberately unpaged) |
| `GET /api/v1/concepts/{id}` | one concept, or the 404 envelope |
| `GET /api/v1/questions` | paginated, filterable question catalogue — see below |

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
      "difficulty": { "value": 3, "code": "MEDIUM" },
      "type": "EXTENDED",
      "createdAt": "2026-06-29T10:15:30Z",
      "linkedConceptIds": [10, 11]
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

## Error handling

All errors aim to return one envelope: `{"error": "...", "status": <code>}` (see
`dto/ErrorResponse`). Each case is an `ExceptionHandler` in `exception/`; Micronaut
routes a thrown exception to the **most specific** handler registered for its type.

Current coverage:

- **400** — `ConversionErrorExceptionHandler` (binding/conversion failures; replaces
  Micronaut's default). For enum-typed params it enumerates the legal values, e.g.
  `?types=BAD` → `"types: must be one of SHORT_ANSWER, EXTENDED, MCQ"` and
  `?difficulties=9` → `"difficulties: must be one of 1(TRIVIAL), 2(EASY), …"`.
- **404** — `NotFoundExceptionHandler` (unmatched route / missing resource).
- **422** — `ConstraintViolationExceptionHandler` (bean-validation failures on an
  otherwise-parseable request, e.g. `@UniqueElements` duplicates), one message per
  violation, sorted and joined.
- **500** — `GenericExceptionHandler` catches any otherwise-unhandled `Exception` as a
  last-resort safety net: consistent envelope, logged at `ERROR`, no internals leaked.
  (An escaping `OptimisticLockException` currently lands here too — pinned by a CT; the
  first write endpoint should introduce a dedicated `409 Conflict` handler.)

One deliberate asymmetry, pinned by CTs: **`Pageable` params never 400.** Micronaut's
binder is lenient — `?page=abc` or `?size=0` silently fall back to the defaults, and
`?size=500` is capped at the configured maximum (50) — whereas filter params
(`?conceptId=abc`) fail loudly with the 400 envelope. Making paging params strict would
need a custom `Pageable` binder; not worth the machinery yet.

## Testing

Three tiers, split by how much of the app is wired and where the boundary is cut.
The guiding rule: **put each test where it can actually observe the behaviour it
claims to verify.** Mocking everything around a thin layer just tests a tautology.

| Tier | Suffix | Wires | Boundary | Task |
|------|--------|-------|----------|------|
| Unit | `*Test` | one class, no context | all deps mocked (Mockito) | `test` |
| Component | `*CT` | real web layer (routing, binding, validation, serialization) | repository / external calls mocked, no DB | `test` |
| Integration | `*IT` | full stack | real Postgres (Testcontainers) | `integrationTest` |

```bash
./gradlew test                  # unit + component (*CT) — the every-change loop
./gradlew integrationTest       # integration (*IT) — real Postgres, pre-merge / CI
./gradlew build                 # runs everything (check depends on integrationTest)
```

**What goes where**

- **Unit** — service logic with deps mocked; and validation *rule correctness* via a bare
  `jakarta.validation.Validator` (no context at all) when a constraint is non-trivial.
- **Component** — controller behaviour: routing, body binding, that `@Valid` is actually
  *wired* (bad payload → 400), JSON serialization. The repository boundary is mocked, so no
  SQL runs. This is the layer that keeps the slow integration tier thin.
- **Integration** — only what genuinely needs a real database: repository queries, `@Query`,
  migrations, transactional behaviour. Kept deliberately few. Organised by what they exercise:
  `integration/db` (schema/constraint behaviour via raw SQL), `integration/repository`
  (repository queries and specifications), `integration/e2e` (full HTTP → DB slices).

**Writing a component test.** Micronaut has no `@WebMvcTest`-style slice annotation, so a
few things have to be arranged by hand to test the web layer without a database. The pattern
lives in `ConceptControllerCT` + `src/test/resources/application-ctslice.properties`:

```java
@ComponentTest                                  // bundles @MicronautTest(transactional = false, environments = "ctslice")
class ConceptControllerCT {

    @Inject EmbeddedServer embeddedServer;       // for RestAssured.port
    @Inject ConceptRepository conceptRepository; // the mock, for stubbing

    @MockBean(ConceptRepository.class)
    ConceptRepository conceptRepository() { return mock(ConceptRepository.class); }

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

**Writing an integration test.** `*IT` tests drive the full stack over real HTTP against a
real Postgres, and arrange their data with **raw SQL**, not the repositories — so a failure
points at the code under test, not at the persistence code used to set it up (and we avoid
adding production methods like `deleteAll` purely for tests). The helper is
`utils.data.TestDatabase` (`insert(table, Map<col,value>)` / `update(table, id, col,
value)` / `clear(table)`); the pattern lives in `ConceptControllerIT`:

```java
@IntegrationTest                                // bundles @MicronautTest(transactional = false)
class ConceptControllerIT {

    @Inject TestDatabase testDatabase;
    @Inject EmbeddedServer embeddedServer;

    @BeforeEach void setUp() {
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
declaratively, supports a shared server, and ships modules for the resources we'll add next
(LocalStack/S3 in Sprint 1.2) and across future services. Hand-rolling Testcontainers per
`*IT` (or per service) to win a Docker-free `test` would trade a small, well-understood cost
for boilerplate that grows with every new IT class, resource type, and microservice. That's
optimising the wrong axis.

**The real fix, when it's worth it.** A genuinely Docker-free fast loop needs Test Resources
*isolated to an integration module* (a separate Gradle module — or a shared convention plugin
once there are multiple services — that applies the plugin, while the unit/component module
does not). A separate source set alone is not enough, because the plugin is project-global.

**Decision: deferred.** With a single `*IT` and ~1s of ryuk startup, the module split is
premature and cuts against the monolith-first stance in `CLAUDE.md` §3. Revisit when the
weight justifies it — **any of**: several `*IT` classes, LocalStack/S3 arriving (Sprint 1.2),
or the first real service extraction (Phase 4, where shared build conventions get set up
anyway). Tracked as `TODO(test-resources)` in `build.gradle.kts`.

What the `ctslice` + `@ComponentTest` machinery buys us today is still real: component tests
do no DB work and express the intended boundary (web layer in, persistence mocked). We keep
it as the design statement, and the module split later turns "no Postgres container" into
"no Docker at all" without changing how the tests are written.

`*IT` tests use a real Postgres 16 via Micronaut Test Resources, so Docker must be available
for `integrationTest`.

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


