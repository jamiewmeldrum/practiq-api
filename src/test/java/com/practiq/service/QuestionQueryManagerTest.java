package com.practiq.service;

import com.practiq.domain.Question;
import com.practiq.domain.projection.LinkedQuestion;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.domain.query.QuestionQuery;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import com.practiq.dto.request.QuestionRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static utils.TestReflection.setField;

// The manager owns the student-catalogue policy, the stable ordering, and the concept-link stitch. It
// returns domain projections (LinkedQuestion), never response DTOs — the mapping to QuestionResponse is a
// separate concern covered by QuestionResponseMapperTest. These tests mock the repositories and the spec
// factory to pin exactly those three responsibilities.
@ExtendWith(MockitoExtension.class)
class QuestionQueryManagerTest {

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
    private QuestionQueryManager questionQueryManager;

    @Test
    void pagedQueryForcesApprovedStatusAndRunsTheBuiltSpecOnASortedPage() {
        QuestionRequest request = new QuestionRequest();
        Pageable requested = Pageable.from(0, 20);
        Pageable ordered = Pageable.from(0, 20, STABLE_ORDER);

        QuestionQuery approvedOnly = catalogueQuery(null, null, null);
        when(questionSpecificationFactory.forQuery(approvedOnly)).thenReturn(SPEC);
        when(questionRepository.findAll(SPEC, ordered)).thenReturn(Page.of(List.of(), ordered, 0L));

        Page<LinkedQuestion> page = questionQueryManager.findStudentVisibleQuestionsPagedAndFiltered(request, requested);

        // The page carries the repository's paging metadata: an empty first page still reports its
        // position, requested size and the (zero) total.
        assertThat(page.getContent().isEmpty(), equalTo(true));
        assertEquals(0, page.getPageNumber());
        assertEquals(20, page.getSize());
        assertEquals(0L, page.getTotalSize());

        // Status pinned to APPROVED, the factory's spec is what runs, against a page carrying the stable
        // created_at,id order rather than the caller's unsorted page. An empty page fires no link query.
        verify(questionSpecificationFactory).forQuery(approvedOnly);
        verify(questionRepository).findAll(SPEC, ordered);
        verifyNoInteractions(questionConceptRepository);
    }

    @Test
    void pagedQueryBuildsQueryFromRequestFiltersAndForcesApprovedStatus() {
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

        questionQueryManager.findStudentVisibleQuestionsPagedAndFiltered(request, requested);

        verify(questionSpecificationFactory).forQuery(expectedQuery);
        verify(questionRepository).findAll(SPEC, ordered);
    }

    @Test
    void pagedQueryStitchesConceptLinksOntoEachQuestion() {
        QuestionRequest request = new QuestionRequest();
        Pageable requested = Pageable.from(0, 20);
        Pageable ordered = Pageable.from(0, 20, STABLE_ORDER);

        long linkedId = 1L;
        Question linked = new Question(
                "Linked body", QuestionDifficulty.MEDIUM, QuestionType.EXTENDED,
                QuestionSource.SEED, QuestionStatus.APPROVED, "AQA GCSE Physics");
        setField(linked, "id", linkedId);

        // A second question with no links proves the stitch defaults to an empty set rather than dropping
        // the question or attaching null.
        long bareId = 2L;
        Question bare = new Question(
                "Bare body", null, null,
                QuestionSource.SEED, QuestionStatus.APPROVED, null);
        setField(bare, "id", bareId);

        QuestionQuery approvedOnly = catalogueQuery(null, null, null);
        when(questionSpecificationFactory.forQuery(approvedOnly)).thenReturn(SPEC);
        when(questionRepository.findAll(SPEC, ordered)).thenReturn(Page.of(List.of(linked, bare), ordered, 2L));

        long conceptA = 10L;
        long conceptB = 11L;
        QuestionConceptLink linkA = new QuestionConceptLink(linkedId, conceptA);
        QuestionConceptLink linkB = new QuestionConceptLink(linkedId, conceptB);
        when(questionConceptRepository.findLinksByQuestionIds(Set.of(linkedId, bareId)))
                .thenReturn(List.of(linkA, linkB));

        Page<LinkedQuestion> page = questionQueryManager.findStudentVisibleQuestionsPagedAndFiltered(request, requested);
        List<LinkedQuestion> content = page.getContent();

        assertEquals(2, content.size());
        assertEquals(2L, page.getTotalSize());

        LinkedQuestion linkedResult = byQuestionId(content, linkedId);
        assertEquals(linked, linkedResult.question());
        assertEquals(Set.of(linkA, linkB), linkedResult.conceptLinks());

        LinkedQuestion bareResult = byQuestionId(content, bareId);
        assertEquals(bare, bareResult.question());
        assertEquals(Set.of(), bareResult.conceptLinks());

        verify(questionSpecificationFactory).forQuery(approvedOnly);
        verify(questionRepository).findAll(SPEC, ordered);
        verify(questionConceptRepository).findLinksByQuestionIds(Set.of(linkedId, bareId));
    }

    @Test
    void findByIdReturnsEmptyWhenNoStudentVisibleQuestionMatches() {
        long id = 1L;

        QuestionQuery byIdQuery = studentVisibleByIdQuery(id);
        when(questionSpecificationFactory.forQuery(byIdQuery)).thenReturn(SPEC);
        when(questionRepository.findOne(SPEC)).thenReturn(Optional.empty());

        Optional<LinkedQuestion> result = questionQueryManager.findStudentVisibleQuestionById(id);

        assertThat(result.isPresent(), equalTo(false));

        verify(questionSpecificationFactory).forQuery(byIdQuery);
        verify(questionRepository).findOne(SPEC);
        verifyNoInteractions(questionConceptRepository);
    }

    @Test
    void findByIdReturnsLinkedQuestionWithItsConceptLinks() {
        long id = 1L;
        Question question = new Question(
                "Question A", QuestionDifficulty.EASY, QuestionType.EXTENDED,
                QuestionSource.GENERATED, QuestionStatus.APPROVED, "GCSE Physics");
        setField(question, "id", id);

        long conceptA = 10L;
        long conceptB = 11L;
        QuestionConceptLink linkA = new QuestionConceptLink(id, conceptA);
        QuestionConceptLink linkB = new QuestionConceptLink(id, conceptB);

        QuestionQuery byIdQuery = studentVisibleByIdQuery(id);
        when(questionSpecificationFactory.forQuery(byIdQuery)).thenReturn(SPEC);
        when(questionRepository.findOne(SPEC)).thenReturn(Optional.of(question));
        when(questionConceptRepository.findLinksByQuestionIds(Set.of(id))).thenReturn(List.of(linkA, linkB));

        Optional<LinkedQuestion> result = questionQueryManager.findStudentVisibleQuestionById(id);

        assertThat(result.isPresent(), equalTo(true));
        LinkedQuestion linkedQuestion = result.get();
        assertEquals(question, linkedQuestion.question());
        assertEquals(Set.of(linkA, linkB), linkedQuestion.conceptLinks());

        verify(questionSpecificationFactory).forQuery(byIdQuery);
        verify(questionRepository).findOne(SPEC);
        verify(questionConceptRepository).findLinksByQuestionIds(Set.of(id));
    }

    @Test
    void doesStudentVisibleQuestionExistReturnsTrueWhenTheSpecMatches() {
        long id = 1L;

        QuestionQuery byIdQuery = studentVisibleByIdQuery(id);
        when(questionSpecificationFactory.forQuery(byIdQuery)).thenReturn(SPEC);
        when(questionRepository.exists(SPEC)).thenReturn(true);

        assertThat(questionQueryManager.doesStudentVisibleQuestionExistForId(id), equalTo(true));

        verify(questionSpecificationFactory).forQuery(byIdQuery);
        verify(questionRepository).exists(SPEC);
        verifyNoInteractions(questionConceptRepository);
    }

    @Test
    void doesStudentVisibleQuestionExistReturnsFalseWhenTheSpecMatchesNothing() {
        long id = 1L;

        QuestionQuery byIdQuery = studentVisibleByIdQuery(id);
        when(questionSpecificationFactory.forQuery(byIdQuery)).thenReturn(SPEC);
        when(questionRepository.exists(SPEC)).thenReturn(false);

        assertThat(questionQueryManager.doesStudentVisibleQuestionExistForId(id), equalTo(false));

        verify(questionRepository).exists(SPEC);
    }

    private static LinkedQuestion byQuestionId(List<LinkedQuestion> linkedQuestions, long id) {
        return linkedQuestions.stream()
                .filter(linkedQuestion -> linkedQuestion.question().getId() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No linked question with id " + id));
    }

    // Hand-built rather than via QuestionQuery.studentCatalogue(...) so the expectation is independent of
    // the production factory: a regression that widens the policy breaks these tests instead of moving with them.
    private QuestionQuery studentVisibleByIdQuery(long questionId) {
        return QuestionQuery.builder()
                .status(QuestionStatus.APPROVED)
                .questionId(questionId)
                .requiresConceptLink(true)
                .build();
    }

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
