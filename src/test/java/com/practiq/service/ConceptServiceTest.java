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

        when(conceptRepository.findAll()).thenReturn(List.of(
                diffraction,
                acceleration
        ));

        List<ConceptDto> concepts = conceptService.get();
        assertThat(concepts, containsInAnyOrder(
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
        when(conceptRepository.findAll()).thenReturn(List.of());

        List<ConceptDto> concepts = conceptService.get();
        assertEquals(0, concepts.size());
    }
}
