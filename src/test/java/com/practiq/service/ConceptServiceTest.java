package com.practiq.service;

import com.practiq.domain.Concept;
import com.practiq.repository.ConceptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptServiceTest {

    @Mock
    private ConceptRepository conceptRepository;

    @InjectMocks
    private ConceptService conceptService;

    @Test
    void getReturnsAllConcepts() {
        Concept diffraction = new Concept(
                "Diffraction",
                "The spreading of waves through a gap or around an obstacle."
        );
        Concept acceleration = new Concept(
                "Acceleration",
                "How the velocity of an object changes over time."
        );

        when(conceptRepository.findAll()).thenReturn(List.of(
                diffraction,
                acceleration
        ));

        List<Concept> concepts = conceptService.get();
        assertEquals(2, concepts.size());
        assertTrue(concepts.contains(diffraction));
        assertTrue(concepts.contains(acceleration));
    }

    @Test
    void getReturnsEmptyListWhenNoneExist() {
        when(conceptRepository.findAll()).thenReturn(List.of());

        List<Concept> concepts = conceptService.get();
        assertEquals(0, concepts.size());
    }
}
