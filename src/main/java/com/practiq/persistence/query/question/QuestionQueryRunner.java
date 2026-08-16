package com.practiq.persistence.query.question;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import com.practiq.persistence.QuestionEntity;
import com.practiq.persistence.projection.QuestionConceptLinkProjection;
import com.practiq.persistence.repository.QuestionConceptRepository;
import com.practiq.persistence.repository.QuestionRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Runs the query it is handed. It does not know who is asking or why — the restrictions arrive already set
// on the QuestionQuery, so a caller wanting the student catalogue must have built one that says so.
@Singleton
public class QuestionQueryRunner {

    // Stable total order for pagination: created_at, then id as a tiebreak so rows can't straddle a page
    // boundary ambiguously. Imposed here because it's a correctness invariant of paging, not a caller choice.
    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    private final QuestionRepository questionRepository;
    private final QuestionConceptRepository questionConceptRepository;
    private final QuestionSpecificationFactory questionSpecificationFactory;

    public QuestionQueryRunner(
            QuestionRepository questionRepository,
            QuestionConceptRepository questionConceptRepository,
            QuestionSpecificationFactory questionSpecificationFactory) {
        this.questionRepository = questionRepository;
        this.questionConceptRepository = questionConceptRepository;
        this.questionSpecificationFactory = questionSpecificationFactory;
    }

    public boolean exists(QuestionQuery query) {
        return questionRepository.exists(specificationFor(query));
    }

    public List<QuestionWithConceptIds> findAll(QuestionQuery query) {
        List<QuestionEntity> questions = questionRepository.findAll(specificationFor(query));
        Map<Long, Set<Long>> conceptIds = conceptIdsByQuestionId(questions);

        return questions.stream()
                .map(question -> withConceptIds(question, conceptIds))
                .toList();
    }

    public Page<QuestionWithConceptIds> findPage(QuestionQuery query, Pageable pageable) {
        Pageable ordered = Pageable.from(pageable.getNumber(), pageable.getSize(), STABLE_ORDER);

        Page<QuestionEntity> page = questionRepository.findAll(specificationFor(query), ordered);
        Map<Long, Set<Long>> conceptIds = conceptIdsByQuestionId(page.getContent());

        return page.map(question -> withConceptIds(question, conceptIds));
    }

    private QuerySpecification<QuestionEntity> specificationFor(QuestionQuery query) {
        return questionSpecificationFactory.forQuery(query);
    }

    private QuestionWithConceptIds withConceptIds(QuestionEntity question, Map<Long, Set<Long>> conceptIds) {
        return new QuestionWithConceptIds(question, conceptIds.getOrDefault(question.getId(), Set.of()));
    }

    // The second half of the two-query stitch: fetch-joining the concept collection into a paged query makes
    // Hibernate page in memory, so the links are read separately and matched up here.
    private Map<Long, Set<Long>> conceptIdsByQuestionId(Collection<QuestionEntity> questions) {
        Set<Long> questionIds = questions.stream().map(QuestionEntity::getId).collect(toSet());
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        return questionConceptRepository.findLinksByQuestionIds(questionIds).stream()
                .collect(groupingBy(
                        QuestionConceptLinkProjection::questionId,
                        mapping(QuestionConceptLinkProjection::conceptId, toSet())));
    }
}
