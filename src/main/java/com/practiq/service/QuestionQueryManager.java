package com.practiq.service;

import com.practiq.domain.Question;
import com.practiq.domain.projection.LinkedQuestion;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.domain.query.QuestionQuery;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.dto.request.QuestionRequest;
import com.practiq.repository.QuestionConceptRepository;
import com.practiq.repository.QuestionRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Singleton
public class QuestionQueryManager {

    // Stable total order for pagination: created_at, then id as a tiebreak so rows can't straddle a page
    // boundary ambiguously. Enforced here because it's a correctness invariant of paging, not a caller choice.
    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    private final QuestionRepository questionRepository;
    private final QuestionConceptRepository questionConceptRepository;
    private final QuestionSpecificationFactory questionSpecificationFactory;

    public QuestionQueryManager(
            QuestionRepository questionRepository,
            QuestionConceptRepository questionConceptRepository,
            QuestionSpecificationFactory questionSpecificationFactory
    ) {
        this.questionRepository = questionRepository;
        this.questionConceptRepository = questionConceptRepository;
        this.questionSpecificationFactory = questionSpecificationFactory;
    }

    public boolean doesStudentVisibleQuestionExistForId(long id) {
        QuerySpecification<Question> spec = studentVisibleQuestionSpec(id);
        return questionRepository.exists(spec);
    }

    public Optional<LinkedQuestion> findStudentVisibleQuestionById(long id) {
        QuerySpecification<Question> spec = studentVisibleQuestionSpec(id);
        return questionRepository.findOne(spec)
                .map(question -> toLinkedQuestion(question, linksByQuestionId(List.of(question))));
    }

    public Page<LinkedQuestion> findStudentVisibleQuestionsPagedAndFiltered(QuestionRequest request, Pageable pageable) {
        QuerySpecification<Question> spec = studentVisibleQuestionSpec(request);
        Pageable ordered = Pageable.from(pageable.getNumber(), pageable.getSize(), STABLE_ORDER);

        Page<Question> page = questionRepository.findAll(spec, ordered);
        List<Question> questions = page.getContent();
        Map<Long, Set<QuestionConceptLink>> linksByQuestionId = linksByQuestionId(questions);

        return page.map(question -> toLinkedQuestion(question, linksByQuestionId));
    }

    private QuerySpecification<Question> studentVisibleQuestionSpec(long id) {
        QuestionQuery query = QuestionQuery.studentCatalogue(id);
        return questionSpecificationFactory.forQuery(query);
    }

    private QuerySpecification<Question> studentVisibleQuestionSpec(QuestionRequest request) {
        QuestionQuery query = QuestionQuery.studentCatalogue(
                request.getTypes(),
                request.getDifficulties(),
                request.getConceptId()
        );
        return questionSpecificationFactory.forQuery(query);
    }

    private Map<Long, Set<QuestionConceptLink>> linksByQuestionId(Collection<Question> questions) {
        Set<Long> questionIds = questions.stream().map(Question::getId).collect(toSet());
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionConceptRepository.findLinksByQuestionIds(questionIds).stream()
                .collect(groupingBy(QuestionConceptLink::questionId, toSet()));
    }

    private LinkedQuestion toLinkedQuestion(Question question, Map<Long, Set<QuestionConceptLink>> linksByQuestionId) {
        return new LinkedQuestion(question, linksByQuestionId.getOrDefault(question.getId(), Set.of()));
    }
}
