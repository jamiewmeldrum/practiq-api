package com.practiq.persistence.query;

import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.persistence.metamodel.SingularAttribute;

public final class SessionTokenQueryRestriction<T> implements QueryRestriction<T, UserRestrictedQuery> {

    private final SingularAttribute<T, String> sessionTokenAttribute;

    public SessionTokenQueryRestriction(SingularAttribute<T, String> sessionTokenAttribute) {
        this.sessionTokenAttribute = sessionTokenAttribute;
    }

    @Override
    public QuerySpecification<T> restrict(UserRestrictedQuery query) {
        String sessionToken = query.sessionToken();
        if (sessionToken == null) {
            throw new IllegalStateException("Session token is required");
        }
        return (root, criteriaQuery, cb) -> cb.equal(root.get(sessionTokenAttribute), sessionToken);
    }
}
