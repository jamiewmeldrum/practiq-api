package com.practiq.domain.query;

import io.micronaut.data.repository.jpa.criteria.QuerySpecification;

/**
 * A mandatory predicate a factory declares rather than a layer it extends. {@code C} is the query
 * capability the restriction reads (e.g. {@link UserRestrictedQuery} for the session token). A factory
 * combines any subset of restrictions; see the query package notes in the README.
 */
public interface QueryRestriction<T, C> {
    QuerySpecification<T> restrict(C query);
}
