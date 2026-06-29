package com.practiq.service;

import com.practiq.domain.Concept;
import com.practiq.dto.ConceptDto;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        Concept diffraction = mock(Concept.class);
        when(diffraction.getId()).thenReturn(diffractionId);
        when(diffraction.getName()).thenReturn(diffractionName);
        when(diffraction.getDescription()).thenReturn(diffractionDescription);
        when(diffraction.getCreatedAt()).thenReturn(diffractionCreatedAt);

        long accelerationId = 43L;
        String accelerationName = "Acceleration";
        String accelerationDescription = "The rate of change of velocity over time.";
        Instant accelerationCreatedAt = Instant.parse("2026-06-29T10:15:30Z");

        Concept acceleration = mock(Concept.class);
        when(acceleration.getId()).thenReturn(accelerationId);
        when(acceleration.getName()).thenReturn(accelerationName);
        when(acceleration.getDescription()).thenReturn(accelerationDescription);
        when(acceleration.getCreatedAt()).thenReturn(accelerationCreatedAt);

        when(conceptRepository.listOrderByCreatedAtAsc()).thenReturn(List.of(
                diffraction,
                acceleration
        ));

        List<ConceptDto> conceptDtos = conceptService.get();
        assertThat(conceptDtos, containsInAnyOrder(
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
    }

    @Test
    void getReturnsEmptyListWhenNoneExist() {
        when(conceptRepository.listOrderByCreatedAtAsc()).thenReturn(List.of());

        List<ConceptDto> concepts = conceptService.get();
        assertEquals(0, concepts.size());
    }

    @Test
    void getReturnsConceptById() {

        long id = 42L;
        String name = "Diffraction";
        String description = "The spreading of waves through a gap or around an obstacle.";
        Instant createdAt = Instant.parse("2026-06-29T10:15:30Z");

        Concept concept = mock(Concept.class);
        when(concept.getId()).thenReturn(id);
        when(concept.getName()).thenReturn(name);
        when(concept.getDescription()).thenReturn(description);
        when(concept.getCreatedAt()).thenReturn(createdAt);

        when(conceptRepository.findById(id)).thenReturn(Optional.of(concept));

        Optional<ConceptDto> conceptDto = conceptService.get(id);
        assertThat(conceptDto.isPresent(), is(true));
        assertThat(conceptDto.get(), allOf(
                        hasProperty("id", equalTo(id)),
                        hasProperty("name", equalTo(name)),
                        hasProperty("description", equalTo(description)),
                        hasProperty("createdAt", equalTo(createdAt))
                )
        );
    }

    @Test
    void getReturnsConceptByIdNotFound() {

        long id = 42L;
        when(conceptRepository.findById(id)).thenReturn(Optional.empty());

        Optional<ConceptDto> conceptDto = conceptService.get(id);
        assertThat(conceptDto.isPresent(), is(false));
    }
}
