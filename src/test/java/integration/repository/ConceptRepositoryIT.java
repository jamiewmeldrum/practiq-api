package integration.repository;

import com.practiq.domain.Concept;
import com.practiq.repository.ConceptRepository;
import utils.IntegrationTest;
import utils.data.QuestionTestData;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.OptimisticLockException;

import java.time.OffsetDateTime;
import java.util.List;

import static utils.TestReflection.setField;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@IntegrationTest
public class ConceptRepositoryIT {

    @Inject
    private ConceptRepository conceptRepository;

    @Inject
    private QuestionTestData data;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void listOrderByCreatedAtAscReturnsConceptsOldestFirst() {
        // created_at is written explicitly (out of insertion order) so the assertion proves the ORDER BY,
        // not the order rows happened to be inserted. Raw SQL can set it even though the entity maps it
        // insertable=false.
        OffsetDateTime earlier = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime later = OffsetDateTime.parse("2026-01-02T00:00:00Z");

        data.concept()
                .name("Later")
                .description("d")
                .createdAt(later)
                .insert();

        data.concept()
                .name("Earlier")
                .description("d")
                .createdAt(earlier)
                .insert();

        List<Concept> concepts = conceptRepository.listOrderByCreatedAtAsc();

        assertThat(concepts.stream().map(Concept::getName).toList(), contains("Earlier", "Later"));
    }

    @Test
    void ensureVersionIncrements() {
        data.concept(1L).insert();

        Concept concept = conceptRepository.findAll().getFirst();
        assertThat(concept.getVersion(), equalTo(0));

        setField(concept, "description", "modified description");
        conceptRepository.update(concept);

        Concept modifiedConcept = conceptRepository.findAll().getFirst();
        assertThat(modifiedConcept.getVersion(), equalTo(1));
    }

    // The other half of @Version: incrementing is only useful if a stale write actually fails. A copy
    // fetched before someone else's update still carries the old version, so writing through it must be
    // rejected rather than silently clobbering the newer row (lost update).
    @Test
    void ensureStaleVersionUpdateIsRejected() {
        data.concept(1L).insert();

        Concept stale = conceptRepository.findAll().getFirst();

        // A concurrent editor wins the race: the row moves to version 1.
        Concept current = conceptRepository.findAll().getFirst();
        setField(current, "description", "Updated first.");
        conceptRepository.update(current);

        // Writing through the stale copy (still version 0) must fail, and the winner's write must survive.
        setField(stale, "description", "Updated second, from stale state.");
        assertThrows(OptimisticLockException.class, () -> conceptRepository.update(stale));

        Concept survivor = conceptRepository.findAll().getFirst();
        assertThat(survivor.getDescription(), equalTo("Updated first."));
        assertThat(survivor.getVersion(), equalTo(1));
    }
}
