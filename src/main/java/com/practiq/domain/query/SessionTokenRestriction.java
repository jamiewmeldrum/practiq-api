package com.practiq.domain.query;

import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.persistence.metamodel.SingularAttribute;

public final class SessionTokenRestriction<T> implements QueryRestriction<T, UserRestrictedQuery> {

    private final SingularAttribute<T, String> sessionTokenAttribute;

    public SessionTokenRestriction(SingularAttribute<T, String> sessionTokenAttribute) {
        this.sessionTokenAttribute = sessionTokenAttribute;
    }

    @Override
    public QuerySpecification<T> restrict(UserRestrictedQuery query) {
        String sessionToken = query.getSessionToken();
        if (sessionToken == null) {
            throw new IllegalStateException("Session token is required");
        }
        return (root, criteriaQuery, cb) -> cb.equal(root.get(sessionTokenAttribute), sessionToken);
    }
}
