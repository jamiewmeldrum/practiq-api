package com.practiq.controller;

import com.practiq.dto.request.QuestionRequest;
import com.practiq.dto.response.PageResponse;
import com.practiq.dto.response.QuestionResponse;
import com.practiq.service.QuestionService;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.RequestBean;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("api/v1/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Get()
    public PageResponse<QuestionResponse> get(@Valid @RequestBean QuestionRequest request, Pageable pageable) {
        log.debug("Requested to GET approved questions");
        return questionService.get(request, pageable);
    }
}
