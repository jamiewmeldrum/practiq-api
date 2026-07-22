package com.practiq.domain.query;

import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utils.CriteriaProbe;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

// SessionTokenRestriction is a reusable component — any factory whose entity carries a session token can
// declare it — so its contract is pinned here, entity-independent (synthetic TestEntity, mocked attribute),
// rather than only through whichever factories currently use it. The spec's calls on the mocked criteria
// environment (CriteriaProbe) are its output at this boundary; what the predicate selects against real rows
// stays with the consuming factory's repository IT.
@ExtendWith(MockitoExtension.class)
class SessionTokenRestrictionTest {

    private static final class TestEntity {
    }

    @Mock
    private SingularAttribute<TestEntity, String> sessionTokenAttribute;

    @Mock
    private Path<String> sessionTokenPath;

    @Mock
    private Predicate equality;

    @Test
    void restrictRejectsANullSessionToken() {
        SessionTokenRestriction<TestEntity> restriction = new SessionTokenRestriction<>(sessionTokenAttribute);

        // Fails fast at query-build time: a null token silently becoming match-nothing (or worse, match-all)
        // would be a session-isolation bypass for every factory that reuses this restriction.
        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> restriction.restrict(() -> null));
        assertEquals("Session token is required", thrown.getMessage());
    }

    @Test
    void restrictBuildsAnEqualityBetweenTheGivenAttributeAndTheQuerysToken() {
        String sessionToken = "session-token";
        CriteriaProbe<TestEntity> probe = new CriteriaProbe<>();
        when(probe.root().get(sessionTokenAttribute)).thenReturn(sessionTokenPath);
        when(probe.criteriaBuilder().equal(sessionTokenPath, sessionToken)).thenReturn(equality);

        QuerySpecification<TestEntity> specification =
                new SessionTokenRestriction<TestEntity>(sessionTokenAttribute).restrict(() -> sessionToken);

        // The resolved predicate IS the equality built from the given attribute and the query's token — the
        // exact-arg stubs above only match if both reached the criteria builder unchanged.
        assertThat(probe.resolve(specification), is(sameInstance(equality)));
    }
}
