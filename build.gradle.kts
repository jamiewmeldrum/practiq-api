plugins {
    id("io.micronaut.application") version "4.6.2"
    id("com.gradleup.shadow") version "8.3.9"
    id("io.micronaut.test-resources") version "4.6.2"
    id("io.micronaut.aot") version "4.6.2"
}

version = "0.1"
group = "com.practiq"



repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("io.micronaut.data:micronaut-data-processor")
    annotationProcessor("io.micronaut:micronaut-http-validation")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
    implementation("io.micronaut:micronaut-management")
    implementation("io.micronaut.data:micronaut-data-hibernate-jpa")
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("io.micronaut.sql:micronaut-hibernate-jpa")
    implementation("io.micronaut.data:micronaut-data-tx-hibernate")
    compileOnly("io.micronaut:micronaut-http-client")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}



application {
    mainClass = "com.practiq.Application"
}

java {
    sourceCompatibility = JavaVersion.toVersion("21")
    targetCompatibility = JavaVersion.toVersion("21")
}




graalvmNative.toolchainDetection = false





micronaut {
    runtime("netty")
    testRuntime("junit5")
    testResources {
        // Docker Engine 29+ enforces a minimum API version of 1.40 and hard-rejects the
        // 1.32 that Testcontainers' bundled docker-java hardcodes for its initial probe.
        // That probe runs in the forked Test Resources service JVM, so the override must
        // target THAT process (serverSystemProperties), not the test task. docker-java
        // reads the `api.version` property (not the DOCKER_API_VERSION env var). 1.40 is
        // the daemon floor and is accepted by every Docker since 19.03.
        // TODO: drop once Micronaut's test-resources ships Testcontainers >= 2.0.2.
        serverSystemProperties.put("api.version", "1.40")

        // Force the Postgres JDBC test-resources module. Auto-inference doesn't add it
        // because the persistence layer (micronaut-data-hibernate-jpa) it keys off is
        // deliberately commented out until Sprint 0.2 — the bare driver alone isn't
        // enough of a signal. Without this, only GenericTestContainerProvider loads and
        // datasources.default.url is never resolved ("No URL specified").
        // TODO: likely redundant once 0.2 re-adds the JPA layer — re-test and drop then.
        additionalModules.add("jdbc-postgresql")
    }
    processing {
        incremental(true)
        annotations("com.practiq.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
    }

}


tasks.named<JavaExec>("run") {
    // Default `./gradlew run` to the `local` environment so the app uses the Compose
    // Postgres in application-local.properties (jdbc:...localhost:5432). Without an active
    // environment the base config has no datasource URL, so Test Resources fills the gap
    // and silently runs the app Update against a throwaway container instead of your real DB.
    // Override on the CLI when needed, e.g. MICRONAUT_ENVIRONMENTS=local,foo ./gradlew run.
    environment("MICRONAUT_ENVIRONMENTS", "local")
}


// Unit (*Test) + component (*CT) tests: the every-change loop. Excludes *IT.
// Component tests are DB-free (no Postgres container — see ConceptControllerCT / README),
// but the Micronaut plugin still attaches the Test Resources service to every Test task,
// so this task currently still starts a ryuk container and needs Docker.
// TODO(test-resources): make `test` fully Docker-free — detach the Test Resources service
//   from this task (likely a dedicated integrationTest source set so only *IT pulls it).
tasks.test {
    useJUnitPlatform()
    exclude("**/*IT.class")
}

// Integration tests (*IT): real Postgres via Testcontainers. Slower — run pre-merge/CI.
val integrationTest = tasks.register<Test>("integrationTest") {
    useJUnitPlatform()
    include("**/*IT.class")
    shouldRunAfter(tasks.test)
}

// `./gradlew build` (CI) runs everything; the dev fast loop is `./gradlew test`.
tasks.check { dependsOn(integrationTest) }

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}







