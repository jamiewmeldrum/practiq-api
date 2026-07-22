package com.practiq.domain.query.question;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static utils.TestReflection.setField;

// The runner is the unit entry point. Its policy (StudentQuestionQueryPolicy) and the QuestionSpecificationFactory
// are real collaborators exercised through it — only the repositories, the DB boundary, are mocked. What the
// student query actually FILTERS (approved + concept-linked) is a property of the executed SQL and is proven in
// integration.repository.QuestionRepositoryIT; that the STUDENT policy imposes those fields is proven directly in
// StudentQuestionQueryPolicyTest. Here we prove the runner's own logic: the two-query concept stitch, the stable
// page order it imposes, and its delegation to the right repository call.
@ExtendWith(MockitoExtension.class)
class StudentQuestionQueryRunnerTest {

    private static final Pageable ORDERED_FIRST_PAGE =
            Pageable.from(0, 20, Sort.of(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionConceptRepository questionConceptRepository;

    private StudentQuestionQueryRunner runner;

    @BeforeEach
    void setUp() {
        // Real policy (built inside the runner) and real spec factory: the query the runner constructs is genuine,
        // only its execution against the DB is mocked.
        runner = new StudentQuestionQueryRunner(
                questionRepository, questionConceptRepository, new QuestionSpecificationFactory());
    }

    @Test
    void findByIdStitchesTheQuestionsConceptLinksOntoIt() {
        long id = 1L;
        Question question = approvedQuestion(id);
        QuestionConceptLink linkA = new QuestionConceptLink(id, 10L);
        QuestionConceptLink linkB = new QuestionConceptLink(id, 11L);

        when(questionRepository.findOne(anySpec())).thenReturn(Optional.of(question));
        when(questionConceptRepository.findLinksByQuestionIds(Set.of(id))).thenReturn(List.of(linkA, linkB));

        Optional<LinkedQuestion> result = runner.findQuestionById(id);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().question(), is(question));
        assertThat(result.get().conceptLinks(), equalTo(Set.of(linkA, linkB)));
    }

    @Test
    void findByIdReturnsEmptyAndFiresNoLinkQueryWhenNothingMatches() {
        when(questionRepository.findOne(anySpec())).thenReturn(Optional.empty());

        assertThat(runner.findQuestionById(1L).isPresent(), is(false));

        verifyNoInteractions(questionConceptRepository);
    }

    @Test
    void pagedQueryStitchesEachQuestionsLinksAndDefaultsToEmptyWhenItHasNone() {
        long linkedId = 1L;
        long bareId = 2L;
        Question linked = approvedQuestion(linkedId);
        Question bare = approvedQuestion(bareId);

        when(questionRepository.findAll(anySpec(), any(Pageable.class)))
                .thenReturn(Page.of(List.of(linked, bare), ORDERED_FIRST_PAGE, 2L));
        QuestionConceptLink link = new QuestionConceptLink(linkedId, 10L);
        when(questionConceptRepository.findLinksByQuestionIds(Set.of(linkedId, bareId))).thenReturn(List.of(link));

        List<LinkedQuestion> content =
                runner.findQuestionsPagedAndFiltered(null, null, null, Pageable.from(0, 20)).getContent();

        // The linked question carries its link; the bare one defaults to an empty set rather than being dropped
        // or carrying null.
        assertThat(linksOf(content, linkedId), equalTo(Set.of(link)));
        assertThat(linksOf(content, bareId), equalTo(Set.of()));
    }

    @Test
    void pagedQueryImposesTheStableCreatedAtThenIdOrder() {
        when(questionRepository.findAll(anySpec(), any(Pageable.class)))
                .thenReturn(Page.of(List.of(), ORDERED_FIRST_PAGE, 0L));

        runner.findQuestionsPagedAndFiltered(null, null, null, Pageable.from(0, 20));

        // The caller's plain page is replaced by one carrying the (created_at, id) order.
        verify(questionRepository).findAll(anySpec(), eq(ORDERED_FIRST_PAGE));
    }

    @Test
    void pagedQueryFiresNoLinkQueryForAnEmptyPage() {
        when(questionRepository.findAll(anySpec(), any(Pageable.class)))
                .thenReturn(Page.of(List.of(), ORDERED_FIRST_PAGE, 0L));

        runner.findQuestionsPagedAndFiltered(null, null, null, Pageable.from(0, 20));

        verifyNoInteractions(questionConceptRepository);
    }

    // Typed matcher so overload resolution picks findAll(QuerySpecification, Pageable)/findOne(QuerySpecification)
    // rather than the Sort-taking siblings (Pageable extends Sort, so an untyped any() is ambiguous). The runner
    // builds the spec from its real factory, so the test can't hold the instance — it only matches on type.
    private static QuerySpecification<Question> anySpec() {
        return any();
    }

    private static Question approvedQuestion(long id) {
        Question question = new Question(
                "Body " + id, QuestionDifficulty.MEDIUM, QuestionType.SHORT_ANSWER,
                QuestionSource.SEED, QuestionStatus.APPROVED, "AQA GCSE Physics");
        setField(question, "id", id);
        return question;
    }

    private static Set<QuestionConceptLink> linksOf(List<LinkedQuestion> content, long questionId) {
        return content.stream()
                .filter(linkedQuestion -> linkedQuestion.question().getId() == questionId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No question with id " + questionId))
                .conceptLinks();
    }
}
