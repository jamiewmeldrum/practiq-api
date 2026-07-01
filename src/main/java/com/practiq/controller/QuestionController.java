package com.practiq.controller;

import com.practiq.dto.QuestionDto;
import com.practiq.service.QuestionService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("api/v1/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Get()
    public List<QuestionDto> get() {
        log.debug("Requested to GET all questions");
        return questionService.get();
    }
}