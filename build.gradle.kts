import net.ltgt.gradle.errorprone.errorprone

plugins {
    idea
    id("io.micronaut.application") version "4.6.2"
    id("com.gradleup.shadow") version "8.3.9"
    id("io.micronaut.test-resources") version "4.6.2"
    id("io.micronaut.aot") version "4.6.2"
    id("com.diffplug.spotless") version "7.2.1"
    id("net.ltgt.errorprone") version "4.3.0"
}

version = "0.1"
group = "com.practiq"

repositories {
    mavenCentral()
}

dependencies {
    // Lombok first: it generates entity accessors before the other processors run.
    annotationProcessor("org.projectlombok:lombok")
    // Generates the JPA static metamodel (Question_, QuestionConcept_, ...) so criteria specs use
    // typed attribute references instead of stringly-typed get("field") calls.
    annotationProcessor("org.hibernate.orm:hibernate-jpamodelgen")
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
    implementation("io.micronaut.aws:micronaut-aws-sdk-v2")
    implementation("io.micronaut.objectstorage:micronaut-object-storage-aws")
    compileOnly("io.micronaut:micronaut-http-client")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    // Micronaut 4 dropped snakeyaml from core and made YAML opt-in. Without it the YAML
    // property-source loader self-disables and every application*.yml is skipped silently.
    runtimeOnly("org.yaml:snakeyaml")
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("io.rest-assured:rest-assured:5.5.0")
    testAnnotationProcessor("io.micronaut:micronaut-inject-java")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Teaches the Test Resources service how to start a LocalStack S3 and what properties to
    // answer with (aws.services.s3.endpoint-override, pointed at the container it started).
    // It goes on `testResourcesService`, not `testImplementation`, because the resolver runs in
    // the forked Test Resources JVM — the tests themselves never see this dependency.
    testResourcesService("io.micronaut.testresources:micronaut-test-resources-localstack-s3")
    errorprone("com.google.errorprone:error_prone_core:2.43.0")
}

application {
    mainClass = "com.practiq.Application"
}

java {
    sourceCompatibility = JavaVersion.toVersion("21")
    targetCompatibility = JavaVersion.toVersion("21")
}

spotless {
    java {
        importOrder()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        palantirJavaFormat()
        formatAnnotations()
    }
    kotlinGradle {
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

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

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        excludedPaths.set(".*/build/generated/.*")
    }
}

tasks.named<JavaExec>("run") {
    // Default `./gradlew run` to the `local` environment so the app uses the Compose
    // Postgres in application-local.yml (jdbc:...localhost:5432). Without an active
    // environment the base config has no datasource URL, so Test Resources fills the gap
    // and silently runs the app Update against a throwaway container instead of your real DB.
    // Override on the CLI when needed, e.g. MICRONAUT_ENVIRONMENTS=local,foo ./gradlew run.
    environment("MICRONAUT_ENVIRONMENTS", "local")
    // See the Test-task block below — same reasoning, same throwaway LocalStack credentials.
    environment("AWS_ACCESS_KEY_ID", "test")
    environment("AWS_SECRET_ACCESS_KEY", "test")
    // Supplied here rather than in application-local.yml so the dev loop resolves the key the same
    // way a deployed environment does — through PRACTIQ_ADMIN_KEY — and so no key literal ships
    // inside the jar's resources. Throwaway: it gates a stand-in check until accounts arrive.
    environment("PRACTIQ_ADMIN_KEY", "local-admin-key")
}

// Unit (*Test) + component (*CT) tests: the every-change loop. Excludes *IT.
// Component tests are DB-free (no Postgres container — see ConceptControllerCT / README),
// but the Micronaut plugin attaches the Test Resources service to every Test task, so this
// task still starts a ryuk container and needs Docker. The plugin's `enabled` flag is
// project-wide (no per-task toggle), so a source set alone can't fix this.
// TODO(test-resources): for a fully Docker-free `test`, isolate Test Resources into a
//   separate integration module (or a shared convention plugin once there are multiple
//   services). Deliberately deferred — see the "deliberate compromise" section in README.md.
tasks.test {
    useJUnitPlatform()
    exclude("**/*IT.class")
    exclude("**/*PT.class")
}

// Integration tests (*IT): real Postgres via Testcontainers. Slower — run pre-merge/CI.
val integrationTest =
    tasks.register<Test>("integrationTest") {
        // ITs share the `test` source set, so wire the classpath explicitly. Gradle 8 deprecated
        // (and 9 removes) the convention that auto-populated these for custom Test tasks.
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        useJUnitPlatform()
        include("**/*IT.class")
        mustRunAfter(tasks.test)
    }

// Performance tests (*PT): assert the per-request JDBC statement count against real Postgres, so an
// eager association or other N+1 regression fails loudly rather than silently slowing a hot path. Real DB
// (Testcontainers), so grouped with the slow tier — never the every-change `test` loop. Shares the test
// source set like integrationTest.
val performanceTest =
    tasks.register<Test>("performanceTest") {
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        useJUnitPlatform()
        include("**/*PT.class")
        mustRunAfter(tasks.test, integrationTest)
    }

// LocalStack credentials for every tier. Deliberately environment variables rather than
// application config: deployed AWS resolves credentials ambiently from the IAM role, so
// supplying them the same way locally keeps one resolution path everywhere. It also covers
// the S3 presigner, which builds its own client inside AwsS3Operations and never sees
// Micronaut config. The values are irrelevant — LocalStack never verifies the signature.
tasks.withType<Test>().configureEach {
    environment("AWS_ACCESS_KEY_ID", "test")
    environment("AWS_SECRET_ACCESS_KEY", "test")
    environment("PRACTIQ_ADMIN_KEY", "b10ef800-3a3a-4395-9bbd-bfe2fa316872")
}

// Local convenience only — CI names each tier explicitly (.github/workflows/ci.yml).
tasks.check {
    dependsOn(integrationTest, performanceTest)
}

graalvmNative.toolchainDetection = false

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}

// IntelliJ's Gradle import intermittently fails to register the annotation-processor output
// (the JPA static metamodel — Question_, QuestionConcept_, ...) as a source root, leaving those
// *_ classes unresolved in the editor even though Gradle compiles fine. Declaring the dirs on the
// idea module makes every Gradle sync mark them as generated source roots authoritatively, so the
// fix survives re-syncs (a manual "Mark as Generated Sources Root" does not).
idea {
    module {
        val apMain = file("build/generated/sources/annotationProcessor/java/main")
        val apTest = file("build/generated/sources/annotationProcessor/java/test")
        sourceDirs = sourceDirs + apMain
        testSources.from(apTest)
        generatedSourceDirs = generatedSourceDirs + apMain + apTest
    }
}
