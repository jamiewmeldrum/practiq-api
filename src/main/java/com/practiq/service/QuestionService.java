package com.practiq.service;

import com.practiq.domain.Question;
import com.practiq.domain.query.QuestionQuery;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.dto.request.QuestionRequest;
import com.practiq.dto.response.PageResponse;
import com.practiq.dto.response.QuestionResponse;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.repository.QuestionConceptRepository;
import com.practiq.repository.QuestionRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.practiq.dto.mapper.QuestionResponseMapper.toQuestionResponse;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Singleton
public class QuestionService {

    private final QuestionQueryManager questionQueryManager;

    public QuestionService(QuestionQueryManager questionQueryManager) {
        this.questionQueryManager = questionQueryManager;
    }

    @Transactional(readOnly = true)
    public Optional<QuestionResponse> get(long id) {
        log.debug("Getting question for id {}", id);
        return questionQueryManager.findQuestionByIdForStudent(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionResponse> get(QuestionRequest request, Pageable pageable) {
        log.debug("Getting approved questions, page {}", pageable.getNumber());
        return questionQueryManager.findQuestionsPagedAndFilteredForStudent(request, pageable);
    }
}
