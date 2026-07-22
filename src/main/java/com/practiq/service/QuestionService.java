package com.practiq.service;

import com.practiq.domain.projection.LinkedQuestion;
import com.practiq.domain.query.question.StudentQuestionQueryRunner;
import com.practiq.dto.mapper.QuestionResponseMapper;
import com.practiq.dto.request.QuestionRequest;
import com.practiq.dto.response.PageResponse;
import com.practiq.dto.response.QuestionResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.practiq.dto.mapper.QuestionResponseMapper.toQuestionResponses;

@Slf4j
@Singleton
public class QuestionService {

    private final StudentQuestionQueryRunner questionQueryRunner;

    public QuestionService(StudentQuestionQueryRunner questionQueryRunner) {
        this.questionQueryRunner = questionQueryRunner;
    }

    @Transactional(readOnly = true)
    public Optional<QuestionResponse> get(long id) {
        log.debug("Getting question for id {}", id);
        return questionQueryRunner.findQuestionById(id)
                .map(QuestionResponseMapper::toQuestionResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionResponse> get(QuestionRequest request, Pageable pageable) {
        log.debug("Getting approved questions, page {}", pageable.getNumber());
        Page<LinkedQuestion> page = questionQueryRunner.findQuestionsPagedAndFiltered(
                request.types(), request.difficulties(), request.conceptId(), pageable);
        return PageResponse.of(page, toQuestionResponses(page.getContent()));
    }
}
