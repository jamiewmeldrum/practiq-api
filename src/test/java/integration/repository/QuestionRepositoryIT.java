package integration.repository;

import com.practiq.domain.Question;
import com.practiq.domain.query.QuestionQuery;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.repository.QuestionRepository;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.QuestionTestData;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static utils.TestReflection.setField;

@IntegrationTest
class QuestionRepositoryIT {

    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    @Inject
    private QuestionRepository questionRepository;

    @Inject
    private QuestionTestData data;

    @Inject
    private QuestionSpecificationFactory questionSpecificationFactory;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void ensureVersionIncrements() {
        data.question()
                .body("A question.")
                .source(QuestionSource.SEED)
                .insert();

        Question question = questionRepository.findAll().getFirst();
        assertThat(question.getVersion(), equalTo(0));

        setField(question, "body", "A modified question.");
        questionRepository.update(question);

        Question modifiedQuestion = questionRepository.findAll().getFirst();
        assertThat(modifiedQuestion.getVersion(), equalTo(1));
    }

    // The other half of @Version: incrementing is only useful if a stale write actually fails. A copy
    // fetched before someone else's update still carries the old version, so writing through it must be
    // rejected rather than silently clobbering the newer row (lost update).
    @Test
    void ensureStaleVersionUpdateIsRejected() {
        data.question()
                .body("A question.")
                .source(QuestionSource.SEED)
                .insert();

        Question stale = questionRepository.findAll().getFirst();

        // A concurrent editor wins the race: the row moves to version 1.
        Question current = questionRepository.findAll().getFirst();
        setField(current, "body", "Updated first.");
        questionRepository.update(current);

        // Writing through the stale copy (still version 0) must fail, and the winner's write must survive.
        setField(stale, "body", "Updated second, from stale state.");
        assertThrows(OptimisticLockException.class, () -> questionRepository.update(stale));

        Question survivor = questionRepository.findAll().getFirst();
        assertThat(survivor.getBody(), equalTo("Updated first."));
        assertThat(survivor.getVersion(), equalTo(1));
    }

    @Test
    void pagedQueryReturnsQuestionsInStableCreatedAtThenIdOrder() {
        OffsetDateTime earlier = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime later = OffsetDateTime.parse("2026-01-02T00:00:00Z");

        long conceptId = 10L;
        data.concept(conceptId).insert();

        // earliestByTime has the highest id but the earliest created_at; the other two share a created_at
        // so only the id tiebreak separates them. Expected order: created_at first (earliestByTime leads),
        // then id ascending within the equal-timestamp pair.
        long earliestByTime = 3L;
        long sameTimeLowId = 1L;
        long sameTimeHighId = 2L;
        data.question(earliestByTime).status(QuestionStatus.APPROVED).createdAt(earlier).insert();
        data.question(sameTimeLowId).status(QuestionStatus.APPROVED).createdAt(later).insert();
        data.question(sameTimeHighId).status(QuestionStatus.APPROVED).createdAt(later).insert();
        data.link(earliestByTime, conceptId).insert();
        data.link(sameTimeLowId, conceptId).insert();
        data.link(sameTimeHighId, conceptId).insert();

        QuestionQuery query = baseQuery()
                .conceptId(conceptId)
                .build();

        QuerySpecification<Question> spec = questionSpecificationFactory.forQuery(query);
        Pageable ordered = Pageable.from(0, 10, STABLE_ORDER);

        List<Question> results = questionRepository.findAll(spec, ordered).getContent();

        assertThat(ids(results), contains(earliestByTime, sameTimeLowId, sameTimeHighId));
    }

    // Every by-id case below carries a second, fully-servable question (id 8). Without it a spec that
    // dropped its id predicate would still answer "found"/"true" from the other row and pass.
    @Test
    void findByIdAndStatusReturnsOptionalEmptyIfNoMatchOnId() {
        long conceptId = 10L;
        data.concept(conceptId).insert();
        servableQuestion(7L, conceptId);
        servableQuestion(8L, conceptId);

        // 9 is neither of the two rows present.
        assertThat(findForQuestionId(9L).isPresent(), is(false));
    }

    @Test
    void findByIdAndStatusReturnsOptionalEmptyIfNotApproved() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long rejectedId = 7L;
        data.question(rejectedId).status(QuestionStatus.REJECTED).insert();
        data.link(rejectedId, conceptId).insert();

        servableQuestion(8L, conceptId);

        assertThat(findForQuestionId(rejectedId).isPresent(), is(false));
    }

    @Test
    void findByIdAndStatusReturnsOptionalEmptyIfNoQuestionConceptLinks() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long unlinkedId = 7L;
        data.question(unlinkedId).status(QuestionStatus.APPROVED).insert();

        servableQuestion(8L, conceptId);

        assertThat(findForQuestionId(unlinkedId).isPresent(), is(false));
    }

    @Test
    void findByIdAndStatusReturnsOptionalQuestionIfMatched() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long id = 7L;
        servableQuestion(id, conceptId);
        servableQuestion(8L, conceptId);

        Optional<Question> question = findForQuestionId(id);

        assertThat(question.isPresent(), is(true));
        assertThat(question.get().getId(), is(id));
        assertThat(question.get().getStatus(), is(QuestionStatus.APPROVED));
    }

    // The visibility gate runs the same studentCatalogue spec through exists(), which Micronaut Data
    // translates differently from findOne() — and the spec carries an EXISTS subquery for the concept
    // link. These mirror the findOne cases so the gate's answer is pinned against a real database rather
    // than only end-to-end through the web layer.
    @Test
    void existsReturnsTrueIfQuestionIsApprovedAndLinked() {
        long conceptId = 10L;
        data.concept(conceptId).insert();
        servableQuestion(7L, conceptId);
        servableQuestion(8L, conceptId);

        assertThat(existsForQuestionId(7L), is(true));
    }

    @Test
    void existsReturnsFalseIfNoQuestionForId() {
        long conceptId = 10L;
        data.concept(conceptId).insert();
        servableQuestion(7L, conceptId);
        servableQuestion(8L, conceptId);

        assertThat(existsForQuestionId(9L), is(false));
    }

    @Test
    void existsReturnsFalseIfQuestionNotApproved() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long rejectedId = 7L;
        data.question(rejectedId).status(QuestionStatus.REJECTED).insert();
        data.link(rejectedId, conceptId).insert();

        servableQuestion(8L, conceptId);

        assertThat(existsForQuestionId(rejectedId), is(false));
    }

    @Test
    void existsReturnsFalseIfQuestionHasNoConceptLinks() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        // The concept exists but is never linked to 7, so the missing link is its only disqualifier.
        long unlinkedId = 7L;
        data.question(unlinkedId).status(QuestionStatus.APPROVED).insert();

        servableQuestion(8L, conceptId);

        assertThat(existsForQuestionId(unlinkedId), is(false));
    }

    private void servableQuestion(long id, long conceptId) {
        data.question(id).status(QuestionStatus.APPROVED).insert();
        data.link(id, conceptId).insert();
    }

    private Optional<Question> findForQuestionId(long questionId) {
        QuestionQuery query = baseQuery().questionId(questionId).build();
        return questionRepository.findOne(questionSpecificationFactory.forQuery(query));
    }

    private boolean existsForQuestionId(long questionId) {
        QuestionQuery query = baseQuery().questionId(questionId).build();
        return questionRepository.exists(questionSpecificationFactory.forQuery(query));
    }

    private static QuestionQuery.QuestionQueryBuilder baseQuery() {
        //Basic catalogue filter - fairly arbitary, but was built for student catalogues so serves as a default
        return QuestionQuery.builder()
                .status(QuestionStatus.APPROVED)
                .requiresConceptLink(true);
    }

    private static List<Long> ids(List<Question> questions) {
        return questions.stream().map(Question::getId).collect(toList());
    }
}
