package integration.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static utils.TestReflection.setField;
import static utils.data.TestData.CONCEPT_DESCRIPTION_MAX_LENGTH;
import static utils.data.TestData.CONCEPT_NAME_MAX_LENGTH;

import com.practiq.persistence.ConceptEntity;
import com.practiq.persistence.repository.ConceptRepository;
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

        List<ConceptEntity> concepts = conceptRepository.listOrderByCreatedAtAsc();

        assertThat(concepts.stream().map(ConceptEntity::getName).toList(), contains("Earlier", "Later"));
    }

    @Test
    void ensureVersionIncrements() {
        data.concept(1L).insert();

        ConceptEntity concept = conceptRepository.findAll().getFirst();
        assertThat(concept.getVersion(), equalTo(0));

        setField(concept, "description", "modified description");
        conceptRepository.update(concept);

        ConceptEntity modifiedConcept = conceptRepository.findAll().getFirst();
        assertThat(modifiedConcept.getVersion(), equalTo(1));
    }

    // The other half of @Version: incrementing is only useful if a stale write actually fails. A copy
    // fetched before someone else's update still carries the old version, so writing through it must be
    // rejected rather than silently clobbering the newer row (lost update).
    @Test
    void ensureStaleVersionUpdateIsRejected() {
        data.concept(1L).insert();

        ConceptEntity stale = conceptRepository.findAll().getFirst();

        // A concurrent editor wins the race: the row moves to version 1.
        ConceptEntity current = conceptRepository.findAll().getFirst();
        setField(current, "description", "Updated first.");
        conceptRepository.update(current);

        // Writing through the stale copy (still version 0) must fail, and the winner's write must survive.
        setField(stale, "description", "Updated second, from stale state.");
        assertThrows(OptimisticLockException.class, () -> conceptRepository.update(stale));

        ConceptEntity survivor = conceptRepository.findAll().getFirst();
        assertThat(survivor.getDescription(), equalTo("Updated first."));
        assertThat(survivor.getVersion(), equalTo(1));
    }

    @Test
    void cannotSaveConceptWithTooLongName() {
        String validName = RandomStringUtils.insecure().nextAlphanumeric(CONCEPT_NAME_MAX_LENGTH);
        ConceptEntity validConcept = conceptRepository.save(new ConceptEntity(validName, "description"));
        assertThat(validConcept.getId(), instanceOf(Long.class));

        String name = RandomStringUtils.insecure().nextAlphanumeric(CONCEPT_NAME_MAX_LENGTH + 1);
        ConceptEntity invalidConcept = new ConceptEntity(name, "description");
        ConstraintViolationException thrown =
                assertThrows(ConstraintViolationException.class, () -> conceptRepository.save(invalidConcept));

        Set<ConstraintViolation<?>> constraintViolations = thrown.getConstraintViolations();
        assertThat(constraintViolations.size(), is(1));
        String message = constraintViolations.stream().findFirst().get().getMessage();
        assertThat(message, equalTo("size must be between 0 and " + CONCEPT_NAME_MAX_LENGTH));
    }

    @Test
    void cannotSaveConceptWithTooLongDescription() {
        String validDescription = RandomStringUtils.insecure().nextAlphanumeric(CONCEPT_DESCRIPTION_MAX_LENGTH);
        ConceptEntity validConcept = conceptRepository.save(new ConceptEntity("name", validDescription));
        assertThat(validConcept.getId(), instanceOf(Long.class));

        String description = RandomStringUtils.insecure().nextAlphanumeric(CONCEPT_DESCRIPTION_MAX_LENGTH + 1);
        ConceptEntity invalidConcept = new ConceptEntity("another name", description);
        ConstraintViolationException thrown =
                assertThrows(ConstraintViolationException.class, () -> conceptRepository.save(invalidConcept));

        Set<ConstraintViolation<?>> constraintViolations = thrown.getConstraintViolations();
        assertThat(constraintViolations.size(), is(1));
        String message = constraintViolations.stream().findFirst().get().getMessage();
        assertThat(message, equalTo("size must be between 0 and " + CONCEPT_DESCRIPTION_MAX_LENGTH));
    }
}
