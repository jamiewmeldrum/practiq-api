package com.practiq.controller;

import com.practiq.dto.ConceptDto;
import com.practiq.service.ConceptService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.util.List;

@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("api/v1/concepts")
public class ConceptController {

    private final ConceptService conceptService;

    public ConceptController(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    @Get()
    public List<ConceptDto> get() {
        return conceptService.get();
    }

    @Get("/{id}")
    public ConceptDto getById(long id) {
        return conceptService.get(id)
                .orElseThrow(NotFoundException::new);
    }
}
