package com.practiq.domain.query;

import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Proves the generic template ONCE, decoupled from any concrete factory via a synthetic entity/query:
// forQuery seeds a conjunction, applies applyDomain, and AND-composes EVERY declared restriction — i.e. a
// restriction is consulted with the query and its predicate actually lands in the final spec, never silently
// dropped. That last property is the invariant SessionTokenRestriction relies on, guaranteed here for all
// factories at once so concrete factories never have to re-prove it.
//
// The check works because Micronaut's spec composition (SpecificationComposition) resolves every composed
// component's toPredicate when the final spec is resolved — so a component that was composed in is invoked,
// and one that was dropped is not. Resolving against mocked criteria lets us observe exactly that.
class QuerySpecificationFactoryTest {

    private static final class TestEntity {
    }

    private record TestQuery(String value) {
    }

    @Test
    @SuppressWarnings("unchecked")
    void forQueryComposesTheDomainSpecWithEveryDeclaredRestriction() {
        TestQuery query = new TestQuery("q");

        QuerySpecification<TestEntity> domainSpec = mock(QuerySpecification.class);
        QuerySpecification<TestEntity> restrictionSpecA = mock(QuerySpecification.class);
        QuerySpecification<TestEntity> restrictionSpecB = mock(QuerySpecification.class);
        QueryRestriction<TestEntity, TestQuery> restrictionA = mock(QueryRestriction.class);
        QueryRestriction<TestEntity, TestQuery> restrictionB = mock(QueryRestriction.class);
        when(restrictionA.restrict(query)).thenReturn(restrictionSpecA);
        when(restrictionB.restrict(query)).thenReturn(restrictionSpecB);

        QuerySpecificationFactory<TestEntity, TestQuery> factory =
                factory(domainSpec, List.of(restrictionA, restrictionB));

        Root<TestEntity> root = mock(Root.class);
        CriteriaQuery<?> criteriaQuery = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        factory.forQuery(query).toPredicate(root, criteriaQuery, cb);

        // Each restriction was consulted with the exact query...
        verify(restrictionA).restrict(query);
        verify(restrictionB).restrict(query);
        // ...and the domain spec plus BOTH restriction specs were resolved as part of the final spec, so none
        // was dropped from the conjunction.
        verify(domainSpec).toPredicate(root, criteriaQuery, cb);
        verify(restrictionSpecA).toPredicate(root, criteriaQuery, cb);
        verify(restrictionSpecB).toPredicate(root, criteriaQuery, cb);
    }

    @Test
    @SuppressWarnings("unchecked")
    void forQueryWithoutRestrictionsAppliesOnlyTheDomain() {
        TestQuery query = new TestQuery("q");
        QuerySpecification<TestEntity> domainSpec = mock(QuerySpecification.class);
        QueryRestriction<TestEntity, TestQuery> unusedRestriction = mock(QueryRestriction.class);

        QuerySpecificationFactory<TestEntity, TestQuery> factory = factory(domainSpec, List.of());

        Root<TestEntity> root = mock(Root.class);
        CriteriaQuery<?> criteriaQuery = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        factory.forQuery(query).toPredicate(root, criteriaQuery, cb);

        verify(domainSpec).toPredicate(root, criteriaQuery, cb);
        verifyNoInteractions(unusedRestriction);
    }

    // Minimal concrete subclass over the synthetic types: applyDomain adds the given spec, restrictions() is
    // whatever the test declares.
    private static QuerySpecificationFactory<TestEntity, TestQuery> factory(
            QuerySpecification<TestEntity> domainSpec,
            List<QueryRestriction<TestEntity, ? super TestQuery>> restrictions) {
        return new QuerySpecificationFactory<>() {
            @Override
            protected QuerySpecification<TestEntity> applyDomain(QuerySpecification<TestEntity> specification, TestQuery query) {
                return specification.and(domainSpec);
            }

            @Override
            protected List<QueryRestriction<TestEntity, ? super TestQuery>> restrictions() {
                return restrictions;
            }
        };
    }
}
