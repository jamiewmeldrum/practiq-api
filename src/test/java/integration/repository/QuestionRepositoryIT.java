package integration.repository;

import com.practiq.domain.Question;
import com.practiq.domain.query.question.QuestionQuery;
import com.practiq.domain.query.question.QuestionSpecificationFactory;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
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

    // --- Specification filtering, run through findAll against a real DB. The spec is part of the repository's
    // DB interaction, so what each constructed QuestionQuery selects is pinned here rather than in a separate
    // spec-factory IT. ---

    @Test
    void forQueryFiltersToTheStatusOnTheQuery() {
        long approvedOneId = 1L;
        long approvedTwoId = 2L;
        long pendingId = 3L;
        long rejectedId = 4L;
        data.question(approvedOneId).status(QuestionStatus.APPROVED).insert();
        data.question(approvedTwoId).status(QuestionStatus.APPROVED).insert();
        data.question(pendingId).status(QuestionStatus.PENDING).insert();
        data.question(rejectedId).status(QuestionStatus.REJECTED).insert();

        // Each status returns only its own rows — the filter is driven by the query's status, not a hard-coded one.
        assertThat(ids(findQuestions(QuestionQuery.builder().status(QuestionStatus.APPROVED).build())),
                containsInAnyOrder(approvedOneId, approvedTwoId));
        assertThat(ids(findQuestions(QuestionQuery.builder().status(QuestionStatus.PENDING).build())),
                containsInAnyOrder(pendingId));
        assertThat(ids(findQuestions(QuestionQuery.builder().status(QuestionStatus.REJECTED).build())),
                containsInAnyOrder(rejectedId));
    }

    @Test
    void forQueryFiltersToTheTypesOnTheQuery() {
        long shortAnswerId = 1L;
        long extendedId = 2L;
        long mcqId = 3L;
        data.question(shortAnswerId).type(QuestionType.SHORT_ANSWER).insert();
        data.question(extendedId).type(QuestionType.EXTENDED).insert();
        data.question(mcqId).type(QuestionType.MCQ).insert();

        QuestionQuery query = QuestionQuery.builder()
                .types(List.of(QuestionType.SHORT_ANSWER, QuestionType.EXTENDED))
                .build();

        assertThat(ids(findQuestions(query)), containsInAnyOrder(shortAnswerId, extendedId));
    }

    @Test
    void forQueryFiltersToTheDifficultiesOnTheQuery() {
        long trivialId = 1L;
        long mediumId = 2L;
        long veryHardId = 3L;
        data.question(trivialId).difficulty(QuestionDifficulty.TRIVIAL).insert();
        data.question(mediumId).difficulty(QuestionDifficulty.MEDIUM).insert();
        data.question(veryHardId).difficulty(QuestionDifficulty.VERY_HARD).insert();

        QuestionQuery query = QuestionQuery.builder()
                .difficulties(List.of(QuestionDifficulty.TRIVIAL, QuestionDifficulty.VERY_HARD))
                .build();

        // The stored integer difficulty (1/3/5) is matched via the attribute converter: MEDIUM (3) is out.
        assertThat(ids(findQuestions(query)), containsInAnyOrder(trivialId, veryHardId));
    }

    @Test
    void forQueryFiltersToTheConceptIdAndCountsSynopticQuestionsOnce() {
        long conceptA = 100L;
        long conceptB = 101L;
        data.concept(conceptA).insert();
        data.concept(conceptB).insert();

        long linkedToA = 1L;
        long linkedToB = 2L;
        long synoptic = 3L;
        data.question(linkedToA).insert();
        data.question(linkedToB).insert();
        data.question(synoptic).insert();
        data.link(linkedToA, conceptA).insert();
        data.link(linkedToB, conceptB).insert();
        data.link(synoptic, conceptA).insert();
        data.link(synoptic, conceptB).insert();

        QuestionQuery query = QuestionQuery.builder().conceptId(conceptA).build();

        // The A-only and synoptic (A+B) questions survive; the synoptic one appears exactly once despite matching
        // two of concept A's link rows — a join would double it and corrupt the count, EXISTS doesn't.
        assertThat(ids(findQuestions(query)), containsInAnyOrder(linkedToA, synoptic));
    }

    @Test
    void forQueryFiltersToTheQuestionIdOnTheQuery() {
        long idA = 1L;
        long idB = 2L;
        data.question(idA).insert();
        data.question(idB).insert();

        QuestionQuery query = QuestionQuery.builder().questionId(idB).build();

        assertThat(ids(findQuestions(query)), contains(idB));
    }

    @Test
    void requiresConceptLinkFiltersOutUnlinkedQuestions() {
        long linkedId = 1L;
        long unlinkedId = 2L;
        data.question(linkedId).insert();
        data.question(unlinkedId).insert();

        long concept = 100L;
        data.concept(concept).insert();
        data.link(linkedId, concept).insert();

        // The flag flips behaviour: off returns both, on drops the unlinked question.
        assertThat(ids(findQuestions(QuestionQuery.builder().requiresConceptLink(false).build())),
                containsInAnyOrder(linkedId, unlinkedId));
        assertThat(ids(findQuestions(QuestionQuery.builder().requiresConceptLink(true).build())),
                containsInAnyOrder(linkedId));
    }

    @Test
    void forQueryAppliesAllFiltersConjunctively() {
        long matches = 1L;
        long wrongType = 2L;
        long wrongStatus = 3L;
        long unlinked = 4L;

        long concept = 100L;
        data.concept(concept).insert();

        data.question(matches).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.question(wrongType).status(QuestionStatus.APPROVED).type(QuestionType.MCQ).difficulty(QuestionDifficulty.EASY).insert();
        data.question(wrongStatus).status(QuestionStatus.PENDING).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.question(unlinked).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.link(matches, concept).insert();
        data.link(wrongType, concept).insert();
        data.link(wrongStatus, concept).insert();

        QuestionQuery query = QuestionQuery.builder()
                .types(List.of(QuestionType.SHORT_ANSWER))
                .difficulties(List.of(QuestionDifficulty.EASY))
                .status(QuestionStatus.APPROVED)
                .conceptId(concept)
                .requiresConceptLink(true)
                .build();

        // Only the row matching every predicate survives: each other row fails exactly one.
        assertThat(ids(findQuestions(query)), containsInAnyOrder(matches));
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

    private List<Question> findQuestions(QuestionQuery query) {
        return questionRepository.findAll(questionSpecificationFactory.forQuery(query));
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
