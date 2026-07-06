package com.practiq.service;

import com.practiq.domain.Question;
import com.practiq.domain.query.QuestionQuery;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.dto.request.QuestionRequest;
import com.practiq.dto.response.QuestionDifficultyResponse;
import com.practiq.dto.response.QuestionResponse;
import com.practiq.repository.QuestionRepository;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final QuestionSpecificationFactory questionSpecificationFactory;

    public QuestionService(QuestionRepository questionRepository, QuestionSpecificationFactory questionSpecificationFactory) {
        this.questionRepository = questionRepository;
        this.questionSpecificationFactory = questionSpecificationFactory;
    }

    public List<QuestionResponse> get(QuestionRequest request) {
        log.debug("Getting all approved questions");

        // Status is hard-coded to APPROVED here, never taken from the request, so an incoming
        // request can never widen what students see. Other filters come off the request.
        QuestionQuery query = new QuestionQuery(request.getTypes(), QuestionStatus.APPROVED);
        QuerySpecification<Question> spec = questionSpecificationFactory.from(query);

        return questionRepository.findAll(spec).stream()
                .map(QuestionService::toQuestionDto)
                .collect(Collectors.toList());
    }

    private static QuestionResponse toQuestionDto(Question question) {
        log.trace("Converting question to QuestionDto: {}", question.getId());

        QuestionDifficulty difficulty = question.getDifficulty();
        Set<Long> conceptIds = question.getConceptLinks().stream()
                .map(link -> link.getId().getConceptId())
                .collect(Collectors.toSet());

        return new QuestionResponse(
                question.getId(),
                question.getBody(),
                difficulty == null ? null : new QuestionDifficultyResponse(difficulty.value(), difficulty.name()),
                question.getType(),
                question.getSource(),
                question.getStatus(),
                question.getSourceSpec(),
                question.getCreatedAt(),
                conceptIds
        );
    }
}
