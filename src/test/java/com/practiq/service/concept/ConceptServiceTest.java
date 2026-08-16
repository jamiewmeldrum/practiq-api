package com.practiq.service.concept;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static utils.TestReflection.setField;

import com.practiq.persistence.ConceptEntity;
import com.practiq.persistence.repository.ConceptRepository;
import com.practiq.service.concept.dto.response.Concept;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConceptServiceTest {

    @Mock
    private ConceptRepository conceptRepository;

    @InjectMocks
    private ConceptService conceptService;

    @Test
    void getReturnsAllConceptsInTheOrderTheRepositoryGaveThem() {
        long diffractionId = 42L;
        int diffractionVersion = 3;
        String diffractionName = "Diffraction";
        String diffractionDescription = "The spreading of waves through a gap or around an obstacle.";
        Instant diffractionCreatedAt = Instant.parse("2026-06-29T10:15:30Z");

        long accelerationId = 43L;
        int accelerationVersion = 0;
        String accelerationName = "Acceleration";
        String accelerationDescription = "The rate of change of velocity over time.";
        Instant accelerationCreatedAt = Instant.parse("2026-06-29T11:15:30Z");

        when(conceptRepository.listOrderByCreatedAtAsc())
                .thenReturn(List.of(
                        conceptEntity(
                                diffractionId,
                                diffractionVersion,
                                diffractionName,
                                diffractionDescription,
                                diffractionCreatedAt),
                        conceptEntity(
                                accelerationId,
                                accelerationVersion,
                                accelerationName,
                                accelerationDescription,
                                accelerationCreatedAt)));

        List<Concept> concepts = conceptService.get();

        // Built here rather than through ConceptMapper: an expected value produced by the code under test
        // moves with it and can never fail.
        assertThat(
                concepts,
                contains(
                        equalTo(new Concept(
                                diffractionId,
                                diffractionVersion,
                                diffractionName,
                                diffractionDescription,
                                diffractionCreatedAt)),
                        equalTo(new Concept(
                                accelerationId,
                                accelerationVersion,
                                accelerationName,
                                accelerationDescription,
                                accelerationCreatedAt))));

        verify(conceptRepository).listOrderByCreatedAtAsc();
    }

    @Test
    void getReturnsEmptyListWhenNoneExist() {
        when(conceptRepository.listOrderByCreatedAtAsc()).thenReturn(List.of());

        List<Concept> concepts = conceptService.get();
        assertEquals(0, concepts.size());

        verify(conceptRepository).listOrderByCreatedAtAsc();
    }

    @Test
    void getByIdReturnsTheConcept() {
        long id = 42L;
        int version = 7;
        String name = "Diffraction";
        String description = "The spreading of waves through a gap or around an obstacle.";
        Instant createdAt = Instant.parse("2026-06-29T10:15:30Z");

        when(conceptRepository.findById(id))
                .thenReturn(Optional.of(conceptEntity(id, version, name, description, createdAt)));

        Optional<Concept> concept = conceptService.getById(id);

        assertThat(concept.isPresent(), is(true));
        assertThat(concept.get(), equalTo(new Concept(id, version, name, description, createdAt)));

        verify(conceptRepository).findById(id);
    }

    @Test
    void getByIdReturnsEmptyWhenNoConceptHasThatId() {
        long id = 42L;
        when(conceptRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Concept> concept = conceptService.getById(id);
        assertThat(concept.isPresent(), is(false));

        verify(conceptRepository).findById(id);
    }

    // A real ConceptEntity rather than a mock: id, version and createdAt are DB-assigned in production, so
    // they're set by reflection. A mocked entity would answer whatever was stubbed for whichever getter the
    // mapper happens to call, so it can't catch the mapper reading the wrong field.
    private static ConceptEntity conceptEntity(
            long id, int version, String name, String description, Instant createdAt) {
        ConceptEntity conceptEntity = new ConceptEntity(name, description);
        setField(conceptEntity, "id", id);
        setField(conceptEntity, "version", version);
        setField(conceptEntity, "createdAt", createdAt);
        return conceptEntity;
    }
}
