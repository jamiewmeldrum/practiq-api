package com.practiq.service.concept;

import com.practiq.persistence.repository.ConceptRepository;
import com.practiq.service.concept.dto.response.Concept;
import com.practiq.service.concept.mapper.ConceptMapper;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ConceptService {

    private final ConceptRepository conceptRepository;

    public ConceptService(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    public List<Concept> get() {
        log.debug("Getting all concepts");
        return conceptRepository.listOrderByCreatedAtAsc().stream()
                .map(ConceptMapper::toConcept)
                .collect(Collectors.toList());
    }

    public Optional<Concept> getById(long id) {
        log.debug("Getting concept by id: {}", id);
        return conceptRepository.findById(id).map(ConceptMapper::toConcept);
    }
}
