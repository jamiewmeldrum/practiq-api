package com.practiq.service;

import com.practiq.domain.Concept;
import com.practiq.domain.Question;
import com.practiq.domain.query.QuestionQuery;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static utils.TestReflection.setField;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    // Sentinel handed back by the mocked factory so we can assert this exact instance reaches the repo.
    // Never executed — the repo is mocked — so the body is irrelevant.
    private static final QuerySpecification<Question> SPEC = (root, query, cb) -> null;
    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionConceptRepository questionConceptRepository;

    @Mock
    private QuestionSpecificationFactory questionSpecificationFactory;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void getForcesApprovedStatusAndRunsTheBuiltSpecOnASortedPage() {
        QuestionRequest request = new QuestionRequest();
        Pageable requested = Pageable.from(0, 20);
        Pageable ordered = Pageable.from(0, 20, STABLE_ORDER);

        QuestionQuery approvedOnly = catalogueQuery(null, null, null);
        when(questionSpecificationFactory.forQuery(approvedOnly)).thenReturn(SPEC);
        when(questionRepository.findAll(SPEC, ordered)).thenReturn(Page.of(List.of(), ordered, 0L));

        PageResponse<QuestionResponse> response = questionService.get(request, requested);

        // The envelope carries the paging metadata off the repository Page: an empty first page still
        // reports its position, requested size and the (zero) total.
        assertEquals(0, response.content().size());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(0L, response.totalCount());

        // Status pinned to APPROVED, the factory's spec is what runs, and it runs against a page carrying
        // the stable created_at,id order rather than the caller's unsorted page.
        verify(questionSpecificationFactory).forQuery(approvedOnly);
        verify(questionRepository).findAll(SPEC, ordered);
    }

    @Test
    void getBuildsQueryFromRequestFiltersAndForcesApprovedStatus() {
        QuestionRequest request = new QuestionRequest();

        List<QuestionType> types = List.of(QuestionType.SHORT_ANSWER, QuestionType.MCQ);
        request.setTypes(types);

        List<QuestionDifficulty> difficulties = List.of(QuestionDifficulty.HARD, QuestionDifficulty.VERY_HARD);
        request.setDifficulties(difficulties);

        request.setConceptId(42L);

        Pageable requested = Pageable.from(0, 20);
        Pageable ordered = Pageable.from(0, 20, STABLE_ORDER);

        // The request's filters reach the factory verbatim inside the student-catalogue query.
        QuestionQuery expectedQuery = catalogueQuery(types, difficulties, 42L);
        when(questionSpecificationFactory.forQuery(expectedQuery)).thenReturn(SPEC);
        when(questionRepository.findAll(SPEC, ordered)).thenReturn(Page.of(List.of(), ordered, 0L));

        questionService.get(request, requested);

        verify(questionSpecificationFactory).forQuery(expectedQuery);
        verify(questionRepository).findAll(SPEC, ordered);
    }

    @Test
    void getStitchesConceptIdsFromTheLinkRepositoryOntoTheResponses() {
        QuestionRequest request = new QuestionRequest();
        Pageable requested = Pageable.from(0, 20);
        Pageable ordered = Pageable.from(0, 20, STABLE_ORDER);

        // A fully-populated, linked question and a bare one (null difficulty/type, no links) — so the same
        // mapping covers the difficulty {value,code} path, the null-difficulty path, and both link shapes.
        long linkedId = 1L;
        QuestionDifficulty difficulty = QuestionDifficulty.MEDIUM;
        Instant linkedCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
        Question linked = new Question(
                "Linked body", difficulty, QuestionType.EXTENDED,
                QuestionSource.SEED, QuestionStatus.APPROVED, "AQA GCSE Physics");
        setField(linked, "id", linkedId);
        setField(linked, "createdAt", linkedCreatedAt);

        long bareId = 2L;
        Instant bareCreatedAt = Instant.parse("2026-01-02T00:00:00Z");
        Question bare = new Question(
                "Bare body", null, null,
                QuestionSource.SEED, QuestionStatus.APPROVED, null);
        setField(bare, "id", bareId);
        setField(bare, "createdAt", bareCreatedAt);

        QuestionQuery approvedOnly = catalogueQuery(null, null, null);
        when(questionSpecificationFactory.forQuery(approvedOnly)).thenReturn(SPEC);
        when(questionRepository.findAll(SPEC, ordered)).thenReturn(Page.of(List.of(linked, bare), ordered, 2L));

        // Only the linked question has concept rows; the bare question's id is absent from the result, which
        // is what drives its linkedConceptIds to an empty set rather than null.
        long conceptA = 10L;
        long conceptB = 11L;
        when(questionConceptRepository.findLinksByQuestionIds(Set.of(linkedId, bareId)))
                .thenReturn(List.of(new QuestionConceptLink(linkedId, conceptA), new QuestionConceptLink(linkedId, conceptB)));

        PageResponse<QuestionResponse> response = questionService.get(request, requested);
        List<QuestionResponse> responses = response.content();

        assertEquals(2, responses.size());
        assertEquals(2L, response.totalCount());

        QuestionResponse linkedResponse = responseById(responses, linkedId);
        assertEquals("Linked body", linkedResponse.getBody());
        assertEquals(difficulty.value(), linkedResponse.getDifficulty().getValue());
        assertEquals(difficulty.name(), linkedResponse.getDifficulty().getCode());
        assertEquals(QuestionType.EXTENDED, linkedResponse.getType());
        assertEquals(linkedCreatedAt, linkedResponse.getCreatedAt());
        assertEquals(Set.of(conceptA, conceptB), linkedResponse.getLinkedConceptIds());

        QuestionResponse bareResponse = responseById(responses, bareId);
        assertEquals("Bare body", bareResponse.getBody());
        assertNull(bareResponse.getDifficulty());
        assertNull(bareResponse.getType());
        assertEquals(bareCreatedAt, bareResponse.getCreatedAt());
        assertEquals(Set.of(), bareResponse.getLinkedConceptIds());

        verify(questionSpecificationFactory).forQuery(approvedOnly);
        verify(questionRepository).findAll(SPEC, ordered);
        verify(questionConceptRepository).findLinksByQuestionIds(Set.of(linkedId, bareId));
    }

    @Test
    void getQuestionByIdReturnsEmptyResponseIfNoMatch() {
        long id = 1L;

        QuestionQuery findQuestionByIdQuery = findQuestionByIdQuery(id);
        when(questionSpecificationFactory.forQuery(findQuestionByIdQuery)).thenReturn(SPEC);
        when(questionRepository.findOne(SPEC)).thenReturn(Optional.empty());

        Optional<QuestionResponse> questionResponse = questionService.get(id);

        assertThat(questionResponse.isPresent(), equalTo(false));

        verify(questionSpecificationFactory).forQuery(findQuestionByIdQuery);
        verify(questionRepository).findOne(SPEC);
    }

    @Test
    void getQuestionByIdReturnsQuestionResponse() {
        long id = 1L;
        String body = "Question A";
        QuestionDifficulty difficulty = QuestionDifficulty.EASY;
        QuestionType type = QuestionType.EXTENDED;
        QuestionSource source = QuestionSource.GENERATED;
        QuestionStatus status = QuestionStatus.APPROVED;
        String sourceSpec = "GCSE Physics";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        Question question = new Question(
                body,
                difficulty,
                type,
                source,
                status,
                sourceSpec
        );
        setField(question, "id", id);
        setField(question, "createdAt", createdAt);

        // Question is linked to two concepts.
        long conceptIdA1 = 10L;
        long conceptIdA2 = 11L;

        Concept concept1 = new Concept("name1", "description1");
        setField(concept1, "id", conceptIdA1);
        Concept concept2 = new Concept("name1", "description1");
        setField(concept2, "id", conceptIdA2);
        List<QuestionConceptLink> links = List.of(
                new QuestionConceptLink(id, conceptIdA1),
                new QuestionConceptLink(id, conceptIdA2)
        );

        QuestionQuery findQuestionByIdQuery = findQuestionByIdQuery(id);
        when(questionSpecificationFactory.forQuery(findQuestionByIdQuery)).thenReturn(SPEC);
        when(questionRepository.findOne(SPEC)).thenReturn(Optional.of(question));
        when(questionConceptRepository.findLinksByQuestionIds(List.of(id))).thenReturn(links);

        Optional<QuestionResponse> questionResponse = questionService.get(id);
        assertThat(questionResponse.isPresent(), equalTo(true));

        QuestionResponse response = questionResponse.get();
        assertEquals(id, response.getId());
        assertEquals(body, response.getBody());
        assertEquals(difficulty.value(), response.getDifficulty().getValue());
        assertEquals(difficulty.name(), response.getDifficulty().getCode());
        assertEquals(type, response.getType());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(Set.of(conceptIdA1, conceptIdA2), response.getLinkedConceptIds());

        verify(questionSpecificationFactory).forQuery(findQuestionByIdQuery);
        verify(questionRepository).findOne(SPEC);
        verify(questionConceptRepository).findLinksByQuestionIds(List.of(id));
    }

    private static QuestionResponse responseById(List<QuestionResponse> responses, long id) {
        return responses.stream()
                .filter(response -> response.getId() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No response with id " + id));
    }

    private QuestionQuery findQuestionByIdQuery(long questionId) {
        return QuestionQuery.builder()
                .status(QuestionStatus.APPROVED)
                .questionId(questionId)
                .requiresConceptLink(true)
                .build();
    }

    // Hand-built rather than via QuestionQuery.studentCatalogue(...) so the expectation is independent of
    // the production factory: a regression that widens the policy breaks this test instead of moving with it.
    private QuestionQuery catalogueQuery(List<QuestionType> types, List<QuestionDifficulty> difficulties, Long conceptId) {
        return QuestionQuery.builder()
                .types(types)
                .difficulties(difficulties)
                .status(QuestionStatus.APPROVED)
                .conceptId(conceptId)
                .requiresConceptLink(true)
                .build();
    }
}
