package com.practiq.service;

import com.practiq.domain.MarkScheme;
import com.practiq.domain.Question;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.domain.query.QuestionQuery;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.dto.mapper.MarkSchemeResponseMapper;
import com.practiq.dto.response.MarkSchemeResponse;
import com.practiq.repository.MarkSchemeRepository;
import com.practiq.repository.QuestionConceptRepository;
import com.practiq.repository.QuestionRepository;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.practiq.dto.mapper.QuestionResponseMapper.toQuestionResponse;
import static java.util.stream.Collectors.toSet;

//TODO - explicit tests
@Slf4j
@Singleton
public class MarkSchemeService {

    private final QuestionRepository questionRepository;
    private final QuestionConceptRepository questionConceptRepository;
    private final QuestionSpecificationFactory questionSpecificationFactory;
    private final MarkSchemeRepository markSchemeRepository;

    public MarkSchemeService(
            QuestionRepository questionRepository,
            QuestionConceptRepository questionConceptRepository,
            QuestionSpecificationFactory questionSpecificationFactory,
            MarkSchemeRepository markSchemeRepository) {

        this.questionRepository = questionRepository;
        this.questionConceptRepository = questionConceptRepository;
        this.questionSpecificationFactory = questionSpecificationFactory;
        this.markSchemeRepository = markSchemeRepository;
    }

    @Transactional(readOnly = true)
    public Optional<MarkSchemeResponse> getForQuestionId(long questionId) {
        log.debug("Getting mark scheme for question id: {}", questionId);

        QuestionQuery query = QuestionQuery.studentCatalogue(questionId);
        QuerySpecification<Question> spec = questionSpecificationFactory.forQuery(query);

        Optional<Question> optionalQuestion = questionRepository.findOne(spec);
        if(optionalQuestion.isEmpty()) {
            return Optional.empty();
        }

        Set<Long> questionConcepts = questionConceptRepository.findLinksByQuestionIds(List.of(questionId)).stream()
                .map(QuestionConceptLink::conceptId)
                .collect(toSet());

        optionalQuestion.map(q -> toQuestionResponse(q, questionConcepts));

        Optional<MarkScheme> markScheme = markSchemeRepository.findByQuestionId(questionId);
        if (markScheme.isEmpty()) {
            return Optional.empty();
        }

        return markScheme.map(MarkSchemeResponseMapper::toMarkSchemeResponse);
    }
}
