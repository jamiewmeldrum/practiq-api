package com.practiq.controller;

import com.practiq.domain.Concept;
import com.practiq.repository.ConceptRepository;
import com.practiq.service.ConceptService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.util.List;

@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("v1/concepts")
public class ConceptController {

    private final ConceptService conceptService;

    ConceptController(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    @Get()
    List<Concept> get() {
        return conceptService.get();
    }
}
