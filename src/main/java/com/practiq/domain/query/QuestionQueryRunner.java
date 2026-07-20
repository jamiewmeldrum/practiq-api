package com.practiq.domain.query;

import com.practiq.domain.Question;
import com.practiq.domain.projection.LinkedQuestion;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.dto.request.QuestionRequest;
import com.practiq.repository.QuestionConceptRepository;
import com.practiq.repository.QuestionRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;

import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

abstract class QuestionQueryRunner <P extends QuestionQueryPolicy> {

    // Stable total order for pagination: created_at, then id as a tiebreak so rows can't straddle a page
    // boundary ambiguously. Enforced here because it's a correctness invariant of paging, not a caller choice.
    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    protected final P policy;

    private final QuestionRepository questionRepository;
    private final QuestionConceptRepository questionConceptRepository;
    private final QuestionSpecificationFactory questionSpecificationFactory;

    protected QuestionQueryRunner(
            QuestionRepository questionRepository,
            QuestionConceptRepository questionConceptRepository,
            QuestionSpecificationFactory questionSpecificationFactory,
            P policy) {
        this.questionRepository = questionRepository;
        this.questionConceptRepository = questionConceptRepository;
        this.questionSpecificationFactory = questionSpecificationFactory;
        this.policy = policy;
    }

    public boolean doesQuestionExistForId(long id) {
        QuestionQuery query = policy.forId(id);
        QuerySpecification<Question> spec = questionSpecificationFactory.forQuery(query);
        return questionRepository.exists(spec);
    }

    public Optional<LinkedQuestion> findQuestionById(long id) {
        QuestionQuery query = policy.forId(id);
        QuerySpecification<Question> spec = questionSpecificationFactory.forQuery(query);
        return questionRepository.findOne(spec)
                .map(question -> toLinkedQuestion(question, linksByQuestionId(List.of(question))));
    }

    public Page<LinkedQuestion> findQuestionsPagedAndFiltered(QuestionRequest request, Pageable pageable) {
        QuestionQuery query = policy.catalogue(
                request.getTypes(),
                request.getDifficulties(),
                request.getConceptId()
        );
        QuerySpecification<Question> spec = questionSpecificationFactory.forQuery(query);

        Pageable ordered = Pageable.from(pageable.getNumber(), pageable.getSize(), STABLE_ORDER);

        Page<Question> page = questionRepository.findAll(spec, ordered);
        List<Question> questions = page.getContent();
        Map<Long, Set<QuestionConceptLink>> linksByQuestionId = linksByQuestionId(questions);

        return page.map(question -> toLinkedQuestion(question, linksByQuestionId));
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
