package com.practiq.service;

import com.practiq.domain.Question;
import com.practiq.domain.query.QuestionQuery;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.dto.request.QuestionRequest;
import com.practiq.dto.response.PageResponse;
import com.practiq.dto.response.QuestionDifficultyResponse;
import com.practiq.dto.response.QuestionResponse;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.repository.QuestionConceptRepository;
import com.practiq.repository.QuestionRepository;
import io.micronaut.core.util.CollectionUtils;
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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Singleton
public class QuestionService {

    // Stable total order for pagination: created_at, then id as a tiebreak so rows can't straddle a page
    // boundary ambiguously. Enforced here because it's a correctness invariant of paging, not a caller choice.
    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    private final QuestionRepository questionRepository;
    private final QuestionConceptRepository questionConceptRepository;
    private final QuestionSpecificationFactory questionSpecificationFactory;

    public QuestionService(QuestionRepository questionRepository, QuestionConceptRepository questionConceptRepository, QuestionSpecificationFactory questionSpecificationFactory) {
        this.questionRepository = questionRepository;
        this.questionConceptRepository = questionConceptRepository;
        this.questionSpecificationFactory = questionSpecificationFactory;
    }

    @Transactional(readOnly = true)
    public Optional<QuestionResponse> get(long id) {
        log.debug("Getting question by id: {}", id);

        Optional<Question> optionalQuestion = questionRepository.findByIdAndStatus(id, QuestionStatus.APPROVED);
        if (optionalQuestion.isEmpty()) {
            return Optional.empty();
        }

        Question question = optionalQuestion.get();
        Set<Long> linkedConceptIds = question.getConceptLinks().stream()
                .map(link -> link.getId().getConceptId())
                .collect(toSet());

        if (CollectionUtils.isEmpty(linkedConceptIds)) {
            return Optional.empty();
        }

        return Optional.of(toQuestionResponse(question, linkedConceptIds));
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionResponse> get(QuestionRequest request, Pageable pageable) {
        log.debug("Getting approved questions, page {}", pageable.getNumber());

        // The student-catalogue policy (APPROVED only, concept-linked only) is fixed by the query's named
        // constructor, never taken from the request — an incoming request can only narrow what students
        // see. Filters come off the request.
        QuestionQuery query = QuestionQuery.studentCatalogue(
                request.getTypes(),
                request.getDifficulties(),
                request.getConceptId()
        );
        QuerySpecification<Question> specification = questionSpecificationFactory.forQuery(query);

        Pageable ordered = Pageable.from(pageable.getNumber(), pageable.getSize(), STABLE_ORDER);
        Page<Question> page = questionRepository.findAll(specification, ordered);
        List<Question> questions = page.getContent();

        // Concept ids are loaded in one flat query keyed by the page's question ids, then stitched on.
        // This replaces the old fetch-join: no row multiplication, no distinct, and the entity's
        // conceptLinks association is never touched (stays lazy).
        Map<Long, Set<Long>> conceptIdsByQuestionId = conceptIdsByQuestionId(questions);

        List<QuestionResponse> responses = questions.stream().map(question ->
                toQuestionResponse(
                        question,
                        conceptIdsByQuestionId.getOrDefault(question.getId(), Set.of()))).collect(toList());

        return PageResponse.of(page, responses);
    }

    private Map<Long, Set<Long>> conceptIdsByQuestionId(List<Question> questions) {
        Set<Long> questionIds = questions.stream().map(Question::getId).collect(toSet());
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionConceptRepository.findLinksByQuestionIds(questionIds).stream()
                .collect(groupingBy(QuestionConceptLink::questionId, mapping(QuestionConceptLink::conceptId, toSet())));
    }

    private static QuestionResponse toQuestionResponse(Question question, Set<Long> conceptIds) {
        log.trace("Converting question to QuestionDto: {}", question.getId());

        QuestionDifficulty difficulty = question.getDifficulty();
        return new QuestionResponse(
                question.getId(),
                question.getBody(),
                difficulty == null ? null : new QuestionDifficultyResponse(difficulty),
                question.getType(),
                question.getCreatedAt(),
                conceptIds
        );
    }
}
