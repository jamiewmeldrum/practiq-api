package com.practiq.service;

import com.practiq.domain.Concept;
import com.practiq.repository.ConceptRepository;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class ConceptService {
    private final ConceptRepository conceptRepository;

    ConceptService(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    public List<Concept> get() {
        return conceptRepository.findAll();
    }
}
