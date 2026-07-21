package com.practiq.domain.query;

import io.micronaut.data.repository.jpa.criteria.QuerySpecification;

import java.util.List;

public abstract class QuerySpecificationFactory<T, U> {

    protected abstract QuerySpecification<T> applyDomain(QuerySpecification<T> specification, U query);

    protected List<QueryRestriction<T, ? super U>> restrictions() {
        return List.of();
    }

    public QuerySpecification<T> forQuery(U query) {
        QuerySpecification<T> specification =
                (root, criteriaQuery, cb) -> cb.conjunction();
        specification = applyDomain(specification, query);
        for (QueryRestriction<T, ? super U> restriction : restrictions()) {
            specification = specification.and(restriction.restrict(query));
        }
        return specification;
    }
}
