# practiq-api

![CI](https://github.com/jamiewmeldrum/practiq-api/actions/workflows/ci.yml/badge.svg)

Adaptive learning/practice platform API (Java 21 · Micronaut 4.10 · PostgreSQL 16).

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

`src/main/resources/db/seed_local.sql` holds sample rows (concepts, and more data
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
duplicate-key errors. Re-running picks up newly added rows and leaves existing ones
untouched. New data types get their own section in the same file following the same
pattern, so loading everything stays a single command.

If you want a clean slate — e.g. to drop rows that are no longer in the file — reset
first, then reload:

```bash
docker exec -i practiq-api-postgres-1 psql -U practiq -d practiq -c 'TRUNCATE concept CASCADE;'
```

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
  migrations, transactional behaviour. Kept deliberately few.

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
`com.practiq.test.TestDatabase` (`insert(table, Map<col,value>)` / `update(table, id, col,
value)` / `clear(table)`); the pattern lives in `ConceptControllerIT`:

```java
@MicronautTest(transactional = false)
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
rows up by name (`find { it.name == … }`) instead of by position — see [Integration tests share
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
calling test's rows.

That means `*IT` tests **must run sequentially**, which they do by default: Gradle's
`maxParallelForks` is `1` and JUnit 5 parallel execution is off. **Don't enable either**
for `integrationTest`. If you do, tests race on the shared table — one test's truncate
wipes the rows another just inserted and is about to read. And because read endpoints
return the whole table and assertions pin the *exact* set returned (`containsInAnyOrder`),
a concurrent writer's rows break the assertion no matter how surgical the cleanup is.
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


