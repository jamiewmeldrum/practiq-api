package com.practiq.service.concept.mapper;

import com.practiq.persistence.ConceptEntity;
import com.practiq.service.concept.dto.response.Concept;

public class ConceptMapper {
    public static Concept toConcept(ConceptEntity conceptEntity) {
        return new Concept(
                conceptEntity.getId(),
                conceptEntity.getVersion(),
                conceptEntity.getName(),
                conceptEntity.getDescription(),
                conceptEntity.getCreatedAt());
    }
}
