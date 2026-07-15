package com.practiq.controller;

import com.practiq.dto.response.MarkSchemeResponse;
import com.practiq.service.MarkSchemeService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("api/v1/questions")
public class MarkSchemeController {

    private final MarkSchemeService markSchemeService;

    public MarkSchemeController(MarkSchemeService markSchemeService) {
        this.markSchemeService = markSchemeService;
    }

    @Get("/{questionId}/mark-scheme")
    public MarkSchemeResponse getForQuestionId(long questionId) {
        log.debug("Requested to GET mark scheme for question id: {}", questionId);
        return markSchemeService.getForQuestionId(questionId)
                .orElseThrow(NotFoundException::new);
    }
}
