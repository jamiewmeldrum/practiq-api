package com.practiq.service;

import com.practiq.domain.Concept;
import com.practiq.dto.response.ConceptResponse;
import com.practiq.repository.ConceptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static utils.TestReflection.setField;

@ExtendWith(MockitoExtension.class)
class ConceptServiceTest {

    @Mock
    private ConceptRepository conceptRepository;

    @InjectMocks
    private ConceptService conceptService;

    @Test
    void getReturnsAllConcepts() {
        long diffractionId = 42L;
        String diffractionName = "Diffraction";
        String diffractionDescription = "The spreading of waves through a gap or around an obstacle.";
        Instant diffractionCreatedAt = Instant.parse("2026-06-29T10:15:30Z");

        Concept diffraction = concept(diffractionId, diffractionName, diffractionDescription, diffractionCreatedAt);

        long accelerationId = 43L;
        String accelerationName = "Acceleration";
        String accelerationDescription = "The rate of change of velocity over time.";
        Instant accelerationCreatedAt = Instant.parse("2026-06-29T10:15:30Z");

        Concept acceleration = concept(accelerationId, accelerationName, accelerationDescription, accelerationCreatedAt);

        when(conceptRepository.listOrderByCreatedAtAsc()).thenReturn(List.of(
                diffraction,
                acceleration
        ));

        List<ConceptResponse> conceptResponses = conceptService.get();
        assertThat(conceptResponses, containsInAnyOrder(
                allOf(
                        hasProperty("id", equalTo(diffractionId)),
                        hasProperty("name", equalTo(diffractionName)),
                        hasProperty("description", equalTo(diffractionDescription)),
                        hasProperty("createdAt", equalTo(diffractionCreatedAt))
                ),
                allOf(
                        hasProperty("id", equalTo(accelerationId)),
                        hasProperty("name", equalTo(accelerationName)),
                        hasProperty("description", equalTo(accelerationDescription)),
                        hasProperty("createdAt", equalTo(accelerationCreatedAt))
                )
        ));

        verify(conceptRepository).listOrderByCreatedAtAsc();
    }

    @Test
    void getReturnsEmptyListWhenNoneExist() {
        when(conceptRepository.listOrderByCreatedAtAsc()).thenReturn(List.of());

        List<ConceptResponse> concepts = conceptService.get();
        assertEquals(0, concepts.size());

        verify(conceptRepository).listOrderByCreatedAtAsc();
    }

    @Test
    void getReturnsConceptById() {
        long id = 42L;
        String name = "Diffraction";
        String description = "The spreading of waves through a gap or around an obstacle.";
        Instant createdAt = Instant.parse("2026-06-29T10:15:30Z");

        when(conceptRepository.findById(id)).thenReturn(Optional.of(concept(id, name, description, createdAt)));

        Optional<ConceptResponse> conceptDto = conceptService.get(id);
        assertThat(conceptDto.isPresent(), is(true));
        assertThat(conceptDto.get(), allOf(
                        hasProperty("id", equalTo(id)),
                        hasProperty("name", equalTo(name)),
                        hasProperty("description", equalTo(description)),
                        hasProperty("createdAt", equalTo(createdAt))
                )
        );

        verify(conceptRepository).findById(id);
    }

    @Test
    void getReturnsConceptByIdNotFound() {
        long id = 42L;
        when(conceptRepository.findById(id)).thenReturn(Optional.empty());

        Optional<ConceptResponse> conceptDto = conceptService.get(id);
        assertThat(conceptDto.isPresent(), is(false));

        verify(conceptRepository).findById(id);
    }

    // A real Concept rather than a mock: id and createdAt are DB-assigned in production, so they're set by
    // reflection. A mocked entity would answer whatever was stubbed for whichever getter the mapper happens
    // to call, so it can't catch the mapper reading the wrong field.
    private static Concept concept(long id, String name, String description, Instant createdAt) {
        Concept concept = new Concept(name, description);
        setField(concept, "id", id);
        setField(concept, "createdAt", createdAt);
        return concept;
    }
}
