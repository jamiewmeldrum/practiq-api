package com.practiq.dto.mapper;

import com.practiq.domain.Concept;
import com.practiq.dto.response.ConceptResponse;
import lombok.extern.slf4j.Slf4j;

//TODO - explicit tests
@Slf4j
public class ConceptResponseMapper {
    public static ConceptResponse toConceptResponse(Concept concept) {
        log.trace("Converting concept to ConceptResponse: {}", concept.getId());
        return new ConceptResponse(
                concept.getId(),
                concept.getName(),
                concept.getDescription(),
                concept.getCreatedAt()
        );
    }
}
