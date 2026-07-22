package com.practiq.controller;

import com.practiq.dto.filter.UserRequestFilter;
import com.practiq.dto.request.QuestionAttemptRequest;
import com.practiq.dto.response.QuestionAttemptResponse;
import com.practiq.http.HttpConstants;
import com.practiq.service.QuestionAttemptService;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("api/v1/questions")
public class QuestionAttemptController {

    //TODO - consider @ValidSessionToken - this will have test knock on effects, so do on new commit

    private final QuestionAttemptService questionAttemptService;

    public QuestionAttemptController(QuestionAttemptService questionAttemptService) {
        this.questionAttemptService = questionAttemptService;
    }

    @Get("/{questionId}/attempts")
    public List<QuestionAttemptResponse> getForQuestionId(
            @NotBlank @Header(HttpConstants.SESSION_TOKEN_HEADER) String sessionToken,
            long questionId
    ) {
        log.debug("Requested to GET question attempts for question id: {}", questionId);

        UserRequestFilter userRequestFilter = new UserRequestFilter(sessionToken);
        return questionAttemptService.getForQuestionId(userRequestFilter, questionId)
                .orElseThrow(NotFoundException::new);
    }

    @Post("/{questionId}/attempts")
    @Status(HttpStatus.CREATED)
    public QuestionAttemptResponse postForQuestionId(
            @NotBlank @Header(HttpConstants.SESSION_TOKEN_HEADER) String sessionToken,
            @Valid @RequestBean QuestionAttemptRequest request,
            long questionId
    ) {
        log.debug("Requested to POST question attempt for question id: {}", questionId);
        log.trace("POST body: {}", request.getBody());

        return questionAttemptService.postForQuestionId(sessionToken, request, questionId)
                .orElseThrow(NotFoundException::new);
    }
}
