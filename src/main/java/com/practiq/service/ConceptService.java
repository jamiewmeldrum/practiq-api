package com.practiq.service;

import com.practiq.dto.mapper.ConceptResponseMapper;
import com.practiq.dto.response.ConceptResponse;
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

    public List<ConceptResponse> get() {
        log.debug("Getting all concepts");
        return conceptRepository.listOrderByCreatedAtAsc().stream()
                .map(ConceptResponseMapper::toConceptResponse)
                .collect(Collectors.toList());
    }

    public Optional<ConceptResponse> get(long id) {
        log.debug("Getting concept by id: {}", id);
        return conceptRepository.findById(id).map(ConceptResponseMapper::toConceptResponse);
    }
}
