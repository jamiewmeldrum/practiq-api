package integration.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static utils.TestReflection.setField;

import com.practiq.domain.Concept;
import com.practiq.repository.ConceptRepository;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.TestData;

@IntegrationTest
class ConceptRepositoryIT {

    @Inject
    private ConceptRepository conceptRepository;

    @Inject
    private TestData data;

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

        data.concept().name("Later").description("d").createdAt(later).insert();

        data.concept().name("Earlier").description("d").createdAt(earlier).insert();

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

    @Test
    void cannotSaveConceptWithTooLongName() {
        // Check 200 chars saves
        String validName = RandomStringUtils.insecure().nextAlphanumeric(200);
        Concept validConcept = conceptRepository.save(new Concept(validName, "description"));
        assertThat(validConcept.getId(), instanceOf(Long.class));

        // Check 201 chars doesn't save
        String name = RandomStringUtils.insecure().nextAlphanumeric(201);
        Concept invalidConcept = new Concept(name, "description");
        ConstraintViolationException thrown =
                assertThrows(ConstraintViolationException.class, () -> conceptRepository.save(invalidConcept));

        Set<ConstraintViolation<?>> constraintViolations = thrown.getConstraintViolations();
        assertThat(constraintViolations.size(), is(1));
        String message = constraintViolations.stream().findFirst().get().getMessage();
        assertThat(message, equalTo("size must be between 0 and 200"));
    }
}
