package utils;

import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.mockito.Mockito.mock;

// A mocked criteria environment for unit-testing a QuerySpecification without a database. A specification is
// an opaque lambda whose only observable surface is toPredicate, so its behaviour becomes visible exactly
// when it is resolved: a test stubs the environment (root().get(...), criteriaBuilder().equal(...)), resolves
// the spec here, and asserts the calls it made — which are its output at this boundary. What the resulting
// predicate SELECTS against real rows is a property of executed SQL and belongs to a repository IT, not here.
@SuppressWarnings("unchecked")
public final class CriteriaProbe<T> {

    private final Root<T> root = mock(Root.class);
    private final CriteriaQuery<?> criteriaQuery = mock(CriteriaQuery.class);
    private final CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

    public Predicate resolve(QuerySpecification<T> specification) {
        return specification.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    public Root<T> root() {
        return root;
    }

    public CriteriaQuery<?> criteriaQuery() {
        return criteriaQuery;
    }

    public CriteriaBuilder criteriaBuilder() {
        return criteriaBuilder;
    }
}
