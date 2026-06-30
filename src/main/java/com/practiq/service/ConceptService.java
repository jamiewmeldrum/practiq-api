package com.practiq.service;

import com.practiq.domain.Concept;
import com.practiq.dto.ConceptDto;
import com.practiq.repository.ConceptRepository;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ConceptService {
    private final ConceptRepository conceptRepository;

    public ConceptService(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    public List<ConceptDto> get() {
        log.debug("Getting all concepts");
        return conceptRepository.listOrderByCreatedAtAsc().stream()
                .map(ConceptService::toConceptDto)
                .collect(Collectors.toList());
    }

    public Optional<ConceptDto> get(long id) {
        log.debug("Getting concept by id: {}", id);
        return conceptRepository.findById(id).map(ConceptService::toConceptDto);
    }

    private static ConceptDto toConceptDto(Concept concept) {
        log.trace("Converting concept to ConceptDto: {}", concept.getId());
        return new ConceptDto(
                concept.getId(),
                concept.getName(),
                concept.getDescription(),
                concept.getCreatedAt()
        );
    }
}
