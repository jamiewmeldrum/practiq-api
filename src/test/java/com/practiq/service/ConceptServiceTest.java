package com.practiq.service;

import com.practiq.repository.ConceptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
class ConceptServiceTest {

    @Mock
    ConceptRepository conceptRepository;

    @InjectMocks
    ConceptService conceptService;

    @Test
    void getReturnsAllConcepts() {
        // TODO: stub conceptRepository.findAll(), call conceptService.get(), assert contents
        fail("not yet implemented");
    }

    @Test
    void getReturnsEmptyListWhenNoneExist() {
        // TODO: stub conceptRepository.findAll() to return empty, assert empty result
        fail("not yet implemented");
    }
}
