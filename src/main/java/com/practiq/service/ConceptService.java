package com.practiq.service;

import com.practiq.domain.Concept;
import com.practiq.dto.ConceptDto;
import com.practiq.repository.ConceptRepository;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ConceptService {
    private final ConceptRepository conceptRepository;

    public ConceptService(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    public List<ConceptDto> get() {
        return conceptRepository.listOrderByCreatedAtAsc().stream()
                .map(concept -> new ConceptDto(
                        concept.getId(),
                        concept.getName(),
                        concept.getDescription(),
                        concept.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
