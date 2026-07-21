package com.practiq.controller;

import com.practiq.dto.filter.UserRequestFilter;
import com.practiq.dto.response.QuestionAttemptResponse;
import com.practiq.http.HttpConstants;
import com.practiq.service.QuestionAttemptService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
            @Header(HttpConstants.SESSION_TOKEN_HEADER) String sessionToken,
            long questionId
    ) {
        log.debug("Requested to GET question attempts for question id: {}", questionId);

        UserRequestFilter userRequestFilter = new UserRequestFilter(sessionToken);
        return questionAttemptService.getForQuestionId(userRequestFilter, questionId)
                .orElseThrow(NotFoundException::new);
    }
}
