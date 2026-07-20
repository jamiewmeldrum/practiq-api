package com.practiq.domain.query;

import com.practiq.domain.Question;
import com.practiq.domain.projection.LinkedQuestion;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static utils.TestReflection.setField;

// Proves the runner mechanics ONCE, policy-agnostic: it asks the policy for a query, builds a spec from
// exactly that query, runs it, stitches concept links, and applies the stable page order. It runs the
// neutral TestQuestionQueryPolicy (which imposes nothing and echoes its inputs), so the expected queries
// here carry no APPROVED / concept-link fields — those are a policy concern, proven in the concrete
// runner/policy tests (e.g. StudentQuestionQueryRunnerTest). Anything true of every policy lives here;
// what a specific policy imposes does not.
@ExtendWith(MockitoExtension.class)
class QuestionQueryRunnerTest {

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
    private TestQuestionQueryRunner runner;

    @Test
    void pagedQueryRunsThePolicysQueryOnAStableSortedPage() {
        Pageable requested = Pageable.from(0, 20);
        Pageable ordered = Pageable.from(0, 20, STABLE_ORDER);

        // Empty request → the policy's catalogue query for (null, null, null). The runner must run exactly
        // that query, against a page carrying the stable created_at,id order rather than the caller's page.
        QuestionQuery policyQuery = catalogueQuery(null, null, null);
        when(questionSpecificationFactory.forQuery(policyQuery)).thenReturn(SPEC);
        when(questionRepository.findAll(SPEC, ordered)).thenReturn(Page.of(List.of(), ordered, 0L));

        Page<LinkedQuestion> page = runner.findQuestionsPagedAndFiltered(null, null, null, requested);

        // The page carries the repository's paging metadata: an empty first page still reports its
        // position, requested size and the (zero) total. An empty page fires no link query.
        assertThat(page.getContent().isEmpty(), equalTo(true));
        assertEquals(0, page.getPageNumber());
        assertEquals(20, page.getSize());
        assertEquals(0L, page.getTotalSize());

        verify(questionSpecificationFactory).forQuery(policyQuery);
        verify(questionRepository).findAll(SPEC, ordered);
        verifyNoInteractions(questionConceptRepository);
    }

    @Test
    void pagedQueryPassesTheRequestFiltersThroughThePolicy() {
        List<QuestionType> types = List.of(QuestionType.SHORT_ANSWER, QuestionType.MCQ);
        List<QuestionDifficulty> difficulties = List.of(QuestionDifficulty.HARD, QuestionDifficulty.VERY_HARD);
        Long conceptId = 42L;

        Pageable requested = Pageable.from(0, 20);
        Pageable ordered = Pageable.from(0, 20, STABLE_ORDER);

        // The filters reach the policy verbatim, and its resulting query is what runs.
        QuestionQuery policyQuery = catalogueQuery(types, difficulties, conceptId);
        when(questionSpecificationFactory.forQuery(policyQuery)).thenReturn(SPEC);
        when(questionRepository.findAll(SPEC, ordered)).thenReturn(Page.of(List.of(), ordered, 0L));

        runner.findQuestionsPagedAndFiltered(types, difficulties, conceptId, requested);

        verify(questionSpecificationFactory).forQuery(policyQuery);
        verify(questionRepository).findAll(SPEC, ordered);
    }

    @Test
    void pagedQueryStitchesConceptLinksOntoEachQuestion() {
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

        QuestionQuery policyQuery = catalogueQuery(null, null, null);
        when(questionSpecificationFactory.forQuery(policyQuery)).thenReturn(SPEC);
        when(questionRepository.findAll(SPEC, ordered)).thenReturn(Page.of(List.of(linked, bare), ordered, 2L));

        long conceptA = 10L;
        long conceptB = 11L;
        QuestionConceptLink linkA = new QuestionConceptLink(linkedId, conceptA);
        QuestionConceptLink linkB = new QuestionConceptLink(linkedId, conceptB);
        when(questionConceptRepository.findLinksByQuestionIds(Set.of(linkedId, bareId)))
                .thenReturn(List.of(linkA, linkB));

        Page<LinkedQuestion> page = runner.findQuestionsPagedAndFiltered(null, null, null, requested);
        List<LinkedQuestion> content = page.getContent();

        assertEquals(2, content.size());
        assertEquals(2L, page.getTotalSize());

        LinkedQuestion linkedResult = byQuestionId(content, linkedId);
        assertEquals(linked, linkedResult.question());
        assertEquals(Set.of(linkA, linkB), linkedResult.conceptLinks());

        LinkedQuestion bareResult = byQuestionId(content, bareId);
        assertEquals(bare, bareResult.question());
        assertEquals(Set.of(), bareResult.conceptLinks());

        verify(questionSpecificationFactory).forQuery(policyQuery);
        verify(questionRepository).findAll(SPEC, ordered);
        verify(questionConceptRepository).findLinksByQuestionIds(Set.of(linkedId, bareId));
    }

    @Test
    void findByIdReturnsEmptyWhenNoQuestionMatches() {
        long id = 1L;

        QuestionQuery policyQuery = byIdQuery(id);
        when(questionSpecificationFactory.forQuery(policyQuery)).thenReturn(SPEC);
        when(questionRepository.findOne(SPEC)).thenReturn(Optional.empty());

        Optional<LinkedQuestion> result = runner.findQuestionById(id);

        assertThat(result.isPresent(), equalTo(false));

        verify(questionSpecificationFactory).forQuery(policyQuery);
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

        QuestionQuery policyQuery = byIdQuery(id);
        when(questionSpecificationFactory.forQuery(policyQuery)).thenReturn(SPEC);
        when(questionRepository.findOne(SPEC)).thenReturn(Optional.of(question));
        when(questionConceptRepository.findLinksByQuestionIds(Set.of(id))).thenReturn(List.of(linkA, linkB));

        Optional<LinkedQuestion> result = runner.findQuestionById(id);

        assertThat(result.isPresent(), equalTo(true));
        LinkedQuestion linkedQuestion = result.get();
        assertEquals(question, linkedQuestion.question());
        assertEquals(Set.of(linkA, linkB), linkedQuestion.conceptLinks());

        verify(questionSpecificationFactory).forQuery(policyQuery);
        verify(questionRepository).findOne(SPEC);
        verify(questionConceptRepository).findLinksByQuestionIds(Set.of(id));
    }

    @Test
    void doesQuestionExistReturnsTrueWhenTheSpecMatches() {
        long id = 1L;

        QuestionQuery policyQuery = byIdQuery(id);
        when(questionSpecificationFactory.forQuery(policyQuery)).thenReturn(SPEC);
        when(questionRepository.exists(SPEC)).thenReturn(true);

        assertThat(runner.doesQuestionExistForId(id), equalTo(true));

        verify(questionSpecificationFactory).forQuery(policyQuery);
        verify(questionRepository).exists(SPEC);
        verifyNoInteractions(questionConceptRepository);
    }

    @Test
    void doesQuestionExistReturnsFalseWhenTheSpecMatchesNothing() {
        long id = 1L;

        QuestionQuery policyQuery = byIdQuery(id);
        when(questionSpecificationFactory.forQuery(policyQuery)).thenReturn(SPEC);
        when(questionRepository.exists(SPEC)).thenReturn(false);

        assertThat(runner.doesQuestionExistForId(id), equalTo(false));

        verify(questionRepository).exists(SPEC);
    }

    // Neutral expected queries, mirroring TestQuestionQueryPolicy's echo — the fixture the runner is driven
    // by, not the production factory. A policy that imposed fields would be a different (concrete) test.
    private QuestionQuery byIdQuery(long questionId) {
        return QuestionQuery.builder()
                .questionId(questionId)
                .build();
    }

    private QuestionQuery catalogueQuery(List<QuestionType> types, List<QuestionDifficulty> difficulties, Long conceptId) {
        return QuestionQuery.builder()
                .types(types)
                .difficulties(difficulties)
                .conceptId(conceptId)
                .build();
    }

    private static LinkedQuestion byQuestionId(List<LinkedQuestion> linkedQuestions, long id) {
        return linkedQuestions.stream()
                .filter(linkedQuestion -> linkedQuestion.question().getId() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No linked question with id " + id));
    }
}
