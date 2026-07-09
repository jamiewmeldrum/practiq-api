package integration.repository;

import com.practiq.domain.Question;
import com.practiq.domain.query.QuestionQuery;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import com.practiq.repository.QuestionRepository;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import utils.IntegrationTest;
import utils.data.QuestionTestData;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

// A QuerySpecification is an opaque lambda — the only faithful way to verify the predicate it adds
// (the status / type / difficulty / concept filters) is to execute it against a real database and
// observe which rows survive. The spec produces WHERE only; ordering is applied via the Pageable's
// (created_at, id) sort in the service, exercised here by the paged-order test.
//
// Every question that should survive a filter is linked to a concept, because a null-conceptId query
// additionally requires at least one link (see forQueryWithNoConceptIdExcludesUnlinkedQuestions): an
// unlinked question is unprocessed and never served, so leaving links off would mask what a filter does.
@IntegrationTest
class QuestionSpecificationFactoryIT {

    private static final long CONCEPT_ID = 100L;
    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    @Inject
    private QuestionTestData data;

    @Inject
    private QuestionRepository questionRepository;

    @Inject
    private QuestionSpecificationFactory questionSpecificationFactory;

    @BeforeEach
    void setUp() {
        data.clear();
        data.concept(CONCEPT_ID).insert();
    }

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
        link(approvedOneId);
        link(approvedTwoId);
        link(pendingId);
        link(rejectedId);

        // Each status returns only its own rows — proving the filter is driven by the query's status
        // value, not a hard-coded one.
        assertThat(ids(findQuestions(query(List.of(), List.of(), QuestionStatus.APPROVED, null))),
                containsInAnyOrder(approvedOneId, approvedTwoId));
        assertThat(ids(findQuestions(query(List.of(), List.of(), QuestionStatus.PENDING, null))),
                containsInAnyOrder(pendingId));
        assertThat(ids(findQuestions(query(List.of(), List.of(), QuestionStatus.REJECTED, null))),
                containsInAnyOrder(rejectedId));
    }

    @Test
    void forQueryFiltersToTheTypesOnTheQuery() {
        long shortAnswerId = 1L;
        long extendedId = 2L;
        long mcqId = 3L;
        // All APPROVED so status can't be what narrows the result — only the type filter can.
        data.question(shortAnswerId).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).insert();
        data.question(extendedId).status(QuestionStatus.APPROVED).type(QuestionType.EXTENDED).insert();
        data.question(mcqId).status(QuestionStatus.APPROVED).type(QuestionType.MCQ).insert();
        link(shortAnswerId);
        link(extendedId);
        link(mcqId);

        QuestionQuery query = query(
                List.of(QuestionType.SHORT_ANSWER, QuestionType.EXTENDED), List.of(), QuestionStatus.APPROVED, null);

        // Only the two requested types come back; the MCQ row is excluded.
        assertThat(ids(findQuestions(query)), containsInAnyOrder(shortAnswerId, extendedId));
    }

    @Test
    void forQueryFiltersToTheDifficultiesOnTheQuery() {
        long trivialId = 1L;
        long mediumId = 2L;
        long veryHardId = 3L;
        // All APPROVED so only the difficulty filter narrows the result.
        data.question(trivialId).status(QuestionStatus.APPROVED).difficulty(QuestionDifficulty.TRIVIAL).insert();
        data.question(mediumId).status(QuestionStatus.APPROVED).difficulty(QuestionDifficulty.MEDIUM).insert();
        data.question(veryHardId).status(QuestionStatus.APPROVED).difficulty(QuestionDifficulty.VERY_HARD).insert();
        link(trivialId);
        link(mediumId);
        link(veryHardId);

        QuestionQuery query = query(
                List.of(), List.of(QuestionDifficulty.TRIVIAL, QuestionDifficulty.VERY_HARD), QuestionStatus.APPROVED, null);

        // The stored integer difficulty (1/3/5) is matched against the requested enum values via the
        // attribute converter: the MEDIUM (3) row is excluded, TRIVIAL (1) and VERY_HARD (5) survive.
        assertThat(ids(findQuestions(query)), containsInAnyOrder(trivialId, veryHardId));
    }

    @Test
    void forQueryFiltersToTheConceptIdOnTheQuery() {
        long conceptA = CONCEPT_ID;
        long conceptB = 101L;
        data.concept(conceptB).insert();

        long linkedToA = 1L;
        long linkedToB = 2L;
        long synoptic = 3L;
        data.question(linkedToA).status(QuestionStatus.APPROVED).insert();
        data.question(linkedToB).status(QuestionStatus.APPROVED).insert();
        data.question(synoptic).status(QuestionStatus.APPROVED).insert();
        data.link(linkedToA, conceptA).insert();
        data.link(linkedToB, conceptB).insert();
        data.link(synoptic, conceptA).insert();
        data.link(synoptic, conceptB).insert();

        QuestionQuery query = query(List.of(), List.of(), QuestionStatus.APPROVED, conceptA);

        // Only questions linked to concept A survive: the A-only question and the synoptic (A+B) one.
        // The B-only question is excluded. Critically the synoptic question appears exactly once despite
        // matching two of concept A's link rows — a join would have returned it twice; EXISTS doesn't
        // multiply rows, so the page count stays honest. (containsInAnyOrder also asserts the exact size,
        // so a duplicate would fail this.)
        assertThat(ids(findQuestions(query)), containsInAnyOrder(linkedToA, synoptic));
    }

    @Test
    void forQueryWithNoConceptIdExcludesUnlinkedQuestions() {
        long linkedId = 1L;
        long unlinkedId = 2L;
        // Both APPROVED; the only difference is the link. A null conceptId means "no concept filter", but
        // an unlinked question is unprocessed and must still never be served.
        data.question(linkedId).status(QuestionStatus.APPROVED).insert();
        data.question(unlinkedId).status(QuestionStatus.APPROVED).insert();
        link(linkedId);

        QuestionQuery query = query(List.of(), List.of(), QuestionStatus.APPROVED, null);

        assertThat(ids(findQuestions(query)), containsInAnyOrder(linkedId));
    }

    @Test
    void forQueryWithoutLinkRequirementReturnsUnlinkedQuestions() {
        long linkedId = 1L;
        long unlinkedId = 2L;
        // The admin-review path: PENDING questions must be visible whether or not they are linked yet —
        // unlinked ones are precisely the ones most in need of review. This pins that dropping the link
        // requirement actually widens the result, before any admin endpoint exists to depend on it.
        data.question(linkedId).status(QuestionStatus.PENDING).insert();
        data.question(unlinkedId).status(QuestionStatus.PENDING).insert();
        link(linkedId);

        QuestionQuery query = new QuestionQuery(List.of(), List.of(), QuestionStatus.PENDING, null, false);

        assertThat(ids(findQuestions(query)), containsInAnyOrder(linkedId, unlinkedId));
    }

    @Test
    void forQueryAppliesAllFiltersConjunctively() {
        // Each row is excluded by exactly one filter, so the sole survivor proves every filter is applied
        // AND-ed together. A row leaking in means the filter that should have dropped it is no longer
        // applied. Extend this by adding a row a new filter is the only one to exclude.
        long matches = 1L;
        long wrongType = 2L;
        long wrongDifficulty = 3L;
        long wrongStatus = 4L;
        long unlinked = 5L;
        data.question(matches).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.question(wrongType).status(QuestionStatus.APPROVED).type(QuestionType.MCQ).difficulty(QuestionDifficulty.EASY).insert();
        data.question(wrongDifficulty).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.HARD).insert();
        data.question(wrongStatus).status(QuestionStatus.PENDING).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.question(unlinked).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        // Every candidate but the unlinked one is linked, so the concept requirement isn't what excludes
        // the type/difficulty/status losers — their own filter is.
        link(matches);
        link(wrongType);
        link(wrongDifficulty);
        link(wrongStatus);

        QuestionQuery query = query(
                List.of(QuestionType.SHORT_ANSWER), List.of(QuestionDifficulty.EASY), QuestionStatus.APPROVED, null);

        assertThat(ids(findQuestions(query)), containsInAnyOrder(matches));
    }

    @Test
    void pagedQueryReturnsQuestionsInStableCreatedAtThenIdOrder() {
        OffsetDateTime earlier = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime later = OffsetDateTime.parse("2026-01-02T00:00:00Z");

        // earliestByTime has the highest id but the earliest created_at; the other two share a created_at
        // so only the id tiebreak separates them. Expected order: created_at first (earliestByTime leads),
        // then id ascending within the equal-timestamp pair.
        long earliestByTime = 3L;
        long sameTimeLowId = 1L;
        long sameTimeHighId = 2L;
        data.question(earliestByTime).status(QuestionStatus.APPROVED).createdAt(earlier).insert();
        data.question(sameTimeLowId).status(QuestionStatus.APPROVED).createdAt(later).insert();
        data.question(sameTimeHighId).status(QuestionStatus.APPROVED).createdAt(later).insert();
        link(earliestByTime);
        link(sameTimeLowId);
        link(sameTimeHighId);

        QuerySpecification<Question> spec = questionSpecificationFactory.forQuery(
                query(List.of(), List.of(), QuestionStatus.APPROVED, null));
        Pageable ordered = Pageable.from(0, 10, STABLE_ORDER);

        List<Question> results = questionRepository.findAll(spec, ordered).getContent();

        assertThat(ids(results), contains(earliestByTime, sameTimeLowId, sameTimeHighId));
    }

    // Links an already-inserted question to the shared concept so it survives the null-conceptId
    // hasConcept requirement. Call after the question row exists (the link FK references it).
    private void link(long questionId) {
        data.link(questionId, CONCEPT_ID).insert();
    }

    private List<Question> findQuestions(QuestionQuery query) {
        return questionRepository.findAll(questionSpecificationFactory.forQuery(query));
    }

    // All standard cases run link-required (the student policy); the admin-path test constructs its
    // no-link-required query directly.
    private static QuestionQuery query(List<QuestionType> types, List<QuestionDifficulty> difficulties,
                                       QuestionStatus status, Long conceptId) {
        return new QuestionQuery(types, difficulties, status, conceptId, true);
    }

    private static List<Long> ids(List<Question> questions) {
        return questions.stream().map(Question::getId).collect(toList());
    }
}
