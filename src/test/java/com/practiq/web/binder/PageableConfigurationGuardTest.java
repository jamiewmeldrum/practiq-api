package com.practiq.web.binder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micronaut.context.ApplicationContext;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Raw contexts rather than a component test: the contract is that the application refuses to start, which
// no tier that requires a started application can observe.
class PageableConfigurationGuardTest {

    private static final String SORT_IGNORE_CASE = "micronaut.data.pageable.sort-ignore-case";
    private static final String SORT_DELIMITER = "micronaut.data.pageable.sort-delimiter";

    @Test
    void theApplicationStartsWhenNoInertSortPropertyIsSet() {
        // The properties the binder does honour are set here too, so this proves the guard objects to the
        // inert pair specifically rather than to any pageable configuration at all.
        try (ApplicationContext context = startWith(
                Map.of("micronaut.data.pageable.max-page-size", 50, "micronaut.data.pageable.default-page-size", 10))) {
            assertDoesNotThrow(() -> context.getBean(PageableQueryBinder.class));
        }
    }

    @Test
    void theApplicationRefusesToStartWhenSortIgnoreCaseIsSet() {
        Exception thrown = assertThrows(Exception.class, () -> startWith(Map.of(SORT_IGNORE_CASE, true)));

        assertThat(rootMessageOf(thrown), containsString(SORT_IGNORE_CASE + " is set but has no effect"));
    }

    @Test
    void theApplicationRefusesToStartWhenSortDelimiterIsSet() {
        Exception thrown = assertThrows(Exception.class, () -> startWith(Map.of(SORT_DELIMITER, ";")));

        assertThat(rootMessageOf(thrown), containsString(SORT_DELIMITER + " is set but has no effect"));
    }

    @Test
    void theApplicationRefusesToStartWhenSortDelimiterIsSetToTheValueItAlreadyHas() {
        // Setting a knob to the value it already carries is still setting a knob that does nothing, and the
        // reader of that config would still expect it to matter.
        Exception thrown = assertThrows(Exception.class, () -> startWith(Map.of(SORT_DELIMITER, ",")));

        assertThat(rootMessageOf(thrown), containsString(SORT_DELIMITER + " is set but has no effect"));
    }

    // ctslice is the environment that cuts persistence, so the context comes up without Flyway, a datasource
    // or Docker — the guard is reached either way.
    private ApplicationContext startWith(Map<String, Object> properties) {
        return ApplicationContext.run(properties, "ctslice");
    }

    private String rootMessageOf(Throwable thrown) {
        Throwable current = thrown;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
