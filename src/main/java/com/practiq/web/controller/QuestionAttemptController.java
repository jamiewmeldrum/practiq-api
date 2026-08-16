package com.practiq.web.controller;

import com.practiq.persistence.QuestionAttemptEntity;
import com.practiq.service.UserRef;
import com.practiq.service.attempt.QuestionAttemptService;
import com.practiq.service.attempt.dto.request.QuestionAttemptCommand;
import com.practiq.web.HttpConstants;
import com.practiq.web.dto.mapper.QuestionAttemptResponseMapper;
import com.practiq.web.dto.request.QuestionAttemptRequest;
import com.practiq.web.dto.response.QuestionAttemptResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("api/v1/questions")
public class QuestionAttemptController {

    private final QuestionAttemptService questionAttemptService;

    public QuestionAttemptController(QuestionAttemptService questionAttemptService) {
        this.questionAttemptService = questionAttemptService;
    }

    @Get("/{questionId}/attempts")
    public List<QuestionAttemptResponse> getForQuestionId(
            @NotBlank @Size(max = QuestionAttemptEntity.SESSION_TOKEN_MAX_LENGTH) @Header(HttpConstants.SESSION_TOKEN_HEADER)
                    String sessionToken,
            @Min(1) long questionId) {
        log.debug("Requested to GET question attempts for question id: {}", questionId);

        return questionAttemptService
                .getForQuestionId(new UserRef(sessionToken), questionId)
                .map(QuestionAttemptResponseMapper::toQuestionAttemptResponses)
                .orElseThrow(NotFoundException::new);
    }

    @Post("/{questionId}/attempts")
    @Status(HttpStatus.CREATED)
    public QuestionAttemptResponse postForQuestionId(
            @NotBlank @Size(max = QuestionAttemptEntity.SESSION_TOKEN_MAX_LENGTH) @Header(HttpConstants.SESSION_TOKEN_HEADER)
                    String sessionToken,
            @Valid @Body QuestionAttemptRequest request,
            @Min(1) long questionId) {
        log.debug("Requested to POST question attempt for question id: {}", questionId);
        log.trace("POST body: {}", request.body());

        QuestionAttemptCommand command =
                new QuestionAttemptCommand(questionId, new UserRef(sessionToken), request.body());

        return questionAttemptService
                .create(command)
                .map(QuestionAttemptResponseMapper::toQuestionAttemptResponse)
                .orElseThrow(NotFoundException::new);
    }
}
