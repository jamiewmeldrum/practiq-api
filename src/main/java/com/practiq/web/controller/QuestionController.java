package com.practiq.web.controller;

import static com.practiq.web.dto.mapper.QuestionResponseMapper.toQuestionResponses;

import com.practiq.service.question.QuestionService;
import com.practiq.service.question.dto.request.QuestionSearchCriteria;
import com.practiq.service.question.dto.response.Question;
import com.practiq.web.dto.mapper.QuestionResponseMapper;
import com.practiq.web.dto.request.QuestionRequest;
import com.practiq.web.dto.response.PageResponse;
import com.practiq.web.dto.response.QuestionResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.RequestBean;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("api/v1/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Get
    public PageResponse<QuestionResponse> get(@Valid @RequestBean QuestionRequest request, Pageable pageable) {
        log.debug("Requested to GET approved questions");
        QuestionSearchCriteria searchCriteria =
                new QuestionSearchCriteria(request.types(), request.difficulties(), request.conceptId());

        Page<Question> pageOfQuestions = questionService.get(searchCriteria, pageable);
        return PageResponse.of(pageOfQuestions, toQuestionResponses(pageOfQuestions.getContent()));
    }

    @Get("/{id}")
    public QuestionResponse getById(@Min(1) long id) {
        log.debug("Requested to GET question by id: {}", id);
        return questionService
                .getById(id)
                .map(QuestionResponseMapper::toQuestionResponse)
                .orElseThrow(NotFoundException::new);
    }
}
