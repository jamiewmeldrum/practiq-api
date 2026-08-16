package com.practiq.web.dto.mapper;

import com.practiq.service.concept.dto.response.Concept;
import com.practiq.web.dto.response.ConceptResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConceptResponseMapper {
    public static ConceptResponse toConceptResponse(Concept concept) {
        log.trace("Converting Concept to ConceptResponse: {}", concept.id());
        return new ConceptResponse(concept.id(), concept.name(), concept.description(), concept.createdAt());
    }
}
