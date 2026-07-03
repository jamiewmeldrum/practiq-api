package com.practiq.controller;

import com.practiq.dto.response.ConceptResponse;
import com.practiq.service.ConceptService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("api/v1/concepts")
public class ConceptController {

    private final ConceptService conceptService;

    public ConceptController(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    @Get()
    public List<ConceptResponse> get() {
        log.debug("Requested to GET all concepts");
        return conceptService.get();
    }

    @Get("/{id}")
    public ConceptResponse getById(long id) {
        log.debug("Requested to GET concept by id: {}", id);
        return conceptService.get(id)
                .orElseThrow(NotFoundException::new);
    }
}
