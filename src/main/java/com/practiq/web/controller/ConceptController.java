package com.practiq.web.controller;

import com.practiq.service.concept.ConceptService;
import com.practiq.web.dto.mapper.ConceptResponseMapper;
import com.practiq.web.dto.response.ConceptResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("api/v1/concepts")
public class ConceptController {

    private final ConceptService conceptService;

    public ConceptController(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    @Get
    public List<ConceptResponse> get() {
        log.debug("Requested to GET all concepts");
        return conceptService.get().stream()
                .map(ConceptResponseMapper::toConceptResponse)
                .collect(Collectors.toList());
    }

    @Get("/{id}")
    public ConceptResponse getById(@Min(1) long id) {
        log.debug("Requested to GET concept by id: {}", id);
        return conceptService
                .getById(id)
                .map(ConceptResponseMapper::toConceptResponse)
                .orElseThrow(NotFoundException::new);
    }
}
