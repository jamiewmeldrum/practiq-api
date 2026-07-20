package com.practiq.controller;

import com.practiq.dto.response.QuestionAttemptResponse;
import com.practiq.service.QuestionAttemptService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import lombok.extern.slf4j.Slf4j;

//TODO - CT testing to shadow MarkSchemeControllerCT
@Slf4j
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("api/v1/questions")
public class QuestionAttemptController {

    private final QuestionAttemptService questionAttemptService;

    public QuestionAttemptController(QuestionAttemptService questionAttemptService) {
        this.questionAttemptService = questionAttemptService;
    }

    @Get("/{questionId}/attempt")
    public QuestionAttemptResponse getForQuestionId(long questionId) {
        log.debug("Requested to GET question attempt for question id: {}", questionId);
        return questionAttemptService.getForQuestionId(questionId)
                .orElseThrow(NotFoundException::new);
    }
}
