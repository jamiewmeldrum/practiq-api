package utils;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Performance tier (*PT): same real-Postgres stack as @IntegrationTest, plus the `performance` profile
// which enables Hibernate statistics so a test can assert the JDBC statement count for a request. Real
// DB, so grouped with the slow tier — not the every-change `test` loop.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@MicronautTest(transactional = false, environments = "performance")
public @interface PerformanceTest {
}
