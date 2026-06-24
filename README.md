# practiq-api

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

## Testing

```bash
./gradlew test                  # unit + context tests
./gradlew integrationTest       # Testcontainers-backed integration tests
```

Both spin up a real Postgres 16 via Micronaut Test Resources — Docker must be available.
No external database or `MICRONAUT_ENVIRONMENTS` is required.

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


