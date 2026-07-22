package integration.query;

import com.practiq.domain.projection.LinkedQuestion;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.domain.query.question.StudentQuestionQueryRunner;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.QuestionTestData;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

// The whole student read path as one unit against a real database: the runner drives its wired
// StudentQuestionQueryPolicy, the QuestionSpecificationFactory it builds, and the two repositories it
// stitches — none of which mean anything without a DB to execute the query. So the serving policy
// (APPROVED + concept-linked), the request filters, the concept-link stitch, and the stable page order are
// all proven here as observable behaviour rather than as which-object-was-passed interaction checks.
//
// This replaces the four seam tests that used to split this path (StudentQuestionQueryPolicyTest,
// StudentQuestionQueryRunnerTest, QuestionQueryRunnerTest, QuestionSpecificationFactoryIT) plus the
// by-id/exists/paging half of QuestionRepositoryIT: one place to look, and it survives any refactor of the
// runner/policy/factory internals that leaves the behaviour intact.
@IntegrationTest
class StudentQuestionQueryRunnerIT {

    private static final Pageable FIRST_PAGE = Pageable.from(0, 20);

    private static final long CONCEPT_ID = 100L;
    private static final long OTHER_CONCEPT_ID = 101L;

    @Inject
    private StudentQuestionQueryRunner runner;

    @Inject
    private QuestionTestData data;

    @BeforeEach
    void setUp() {
        data.clear();
        data.concept(CONCEPT_ID).insert();
        data.concept(OTHER_CONCEPT_ID).insert();
    }

    // --- catalogue: serving policy + filters ---

    @Test
    void catalogueServesOnlyApprovedAndConceptLinkedQuestions() {
        long approvedLinkedOne = 1L;
        long approvedLinkedTwo = 2L;
        servable(approvedLinkedOne);
        servable(approvedLinkedTwo);

        // Each disqualifier present exactly once: wrong status (even when linked) and no link (even when
        // approved) are both excluded, so neither half of the policy can be dropped without this failing.
        long pendingButLinked = 3L;
        data.question(pendingButLinked).status(QuestionStatus.PENDING).insert();
        data.link(pendingButLinked, CONCEPT_ID).insert();

        long rejectedButLinked = 4L;
        data.question(rejectedButLinked).status(QuestionStatus.REJECTED).insert();
        data.link(rejectedButLinked, CONCEPT_ID).insert();

        long approvedButUnlinked = 5L;
        data.question(approvedButUnlinked).status(QuestionStatus.APPROVED).insert();

        assertThat(ids(catalogue()), containsInAnyOrder(approvedLinkedOne, approvedLinkedTwo));
    }

    @Test
    void catalogueFiltersByType() {
        long shortAnswer = 1L;
        long extended = 2L;
        long mcq = 3L;
        data.question(shortAnswer).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).insert();
        data.question(extended).status(QuestionStatus.APPROVED).type(QuestionType.EXTENDED).insert();
        data.question(mcq).status(QuestionStatus.APPROVED).type(QuestionType.MCQ).insert();
        data.link(shortAnswer, CONCEPT_ID).insert();
        data.link(extended, CONCEPT_ID).insert();
        data.link(mcq, CONCEPT_ID).insert();

        Page<LinkedQuestion> page = runner.findQuestionsPagedAndFiltered(
                List.of(QuestionType.SHORT_ANSWER, QuestionType.EXTENDED), null, null, FIRST_PAGE);

        assertThat(ids(page), containsInAnyOrder(shortAnswer, extended));
    }

    @Test
    void catalogueFiltersByDifficulty() {
        long trivial = 1L;
        long medium = 2L;
        long veryHard = 3L;
        data.question(trivial).status(QuestionStatus.APPROVED).difficulty(QuestionDifficulty.TRIVIAL).insert();
        data.question(medium).status(QuestionStatus.APPROVED).difficulty(QuestionDifficulty.MEDIUM).insert();
        data.question(veryHard).status(QuestionStatus.APPROVED).difficulty(QuestionDifficulty.VERY_HARD).insert();
        data.link(trivial, CONCEPT_ID).insert();
        data.link(medium, CONCEPT_ID).insert();
        data.link(veryHard, CONCEPT_ID).insert();

        Page<LinkedQuestion> page = runner.findQuestionsPagedAndFiltered(
                null, List.of(QuestionDifficulty.TRIVIAL, QuestionDifficulty.VERY_HARD), null, FIRST_PAGE);

        // The stored integer difficulty (1/3/5) is matched via the attribute converter: MEDIUM (3) is out.
        assertThat(ids(page), containsInAnyOrder(trivial, veryHard));
    }

    @Test
    void catalogueFiltersByConceptIdAndCountsSynopticQuestionsOnce() {
        long linkedToConcept = 1L;
        long linkedToOther = 2L;
        long synoptic = 3L;
        servable(linkedToConcept);
        servable(linkedToOther, OTHER_CONCEPT_ID);
        servable(synoptic);
        data.link(synoptic, OTHER_CONCEPT_ID).insert();

        Page<LinkedQuestion> page = runner.findQuestionsPagedAndFiltered(null, null, CONCEPT_ID, FIRST_PAGE);

        // Only questions linked to CONCEPT_ID survive, and the synoptic one appears exactly once despite
        // matching two link rows — a join would double it and corrupt the count; the EXISTS subquery doesn't.
        assertThat(ids(page), containsInAnyOrder(linkedToConcept, synoptic));
        assertThat(page.getTotalSize(), is(2L));
    }

    @Test
    void catalogueStitchesEachQuestionsOwnConceptLinks() {
        long linkedToConcept = 1L;
        long linkedToOther = 2L;
        long synoptic = 3L;
        servable(linkedToConcept);
        servable(linkedToOther, OTHER_CONCEPT_ID);
        servable(synoptic);
        data.link(synoptic, OTHER_CONCEPT_ID).insert();

        List<LinkedQuestion> content = catalogue().getContent();

        // Each question carries its own links and only its own — the stitch groups by question id rather than
        // smearing one question's links across the page.
        assertThat(linksOf(content, linkedToConcept), equalTo(Set.of(new QuestionConceptLink(linkedToConcept, CONCEPT_ID))));
        assertThat(linksOf(content, linkedToOther), equalTo(Set.of(new QuestionConceptLink(linkedToOther, OTHER_CONCEPT_ID))));
        assertThat(linksOf(content, synoptic), equalTo(Set.of(
                new QuestionConceptLink(synoptic, CONCEPT_ID),
                new QuestionConceptLink(synoptic, OTHER_CONCEPT_ID))));
    }

    @Test
    void catalogueReturnsResultsInStableCreatedAtThenIdOrder() {
        OffsetDateTime earlier = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime later = OffsetDateTime.parse("2026-01-02T00:00:00Z");

        // earliestByTime has the highest id but the earliest created_at; the other two share a created_at so
        // only the id tiebreak separates them. Expected: created_at first, then id ascending within the tie.
        long earliestByTime = 3L;
        long sameTimeLowId = 1L;
        long sameTimeHighId = 2L;
        data.question(earliestByTime).status(QuestionStatus.APPROVED).createdAt(earlier).insert();
        data.question(sameTimeLowId).status(QuestionStatus.APPROVED).createdAt(later).insert();
        data.question(sameTimeHighId).status(QuestionStatus.APPROVED).createdAt(later).insert();
        data.link(earliestByTime, CONCEPT_ID).insert();
        data.link(sameTimeLowId, CONCEPT_ID).insert();
        data.link(sameTimeHighId, CONCEPT_ID).insert();

        assertThat(ids(catalogue()), contains(earliestByTime, sameTimeLowId, sameTimeHighId));
    }

    // --- findById ---

    @Test
    void findByIdReturnsTheApprovedLinkedQuestionWithItsConceptLinks() {
        long wanted = 7L;
        long other = 8L;
        servable(wanted);
        servable(other);

        Optional<LinkedQuestion> result = runner.findQuestionById(wanted);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().question().getId(), is(wanted));
        assertThat(result.get().conceptLinks(), equalTo(Set.of(new QuestionConceptLink(wanted, CONCEPT_ID))));
    }

    @Test
    void findByIdReturnsEmptyWhenTheQuestionIsNotApproved() {
        long rejected = 7L;
        data.question(rejected).status(QuestionStatus.REJECTED).insert();
        data.link(rejected, CONCEPT_ID).insert();
        servable(8L);

        assertThat(runner.findQuestionById(rejected).isPresent(), is(false));
    }

    @Test
    void findByIdReturnsEmptyWhenTheQuestionHasNoConceptLink() {
        long unlinked = 7L;
        data.question(unlinked).status(QuestionStatus.APPROVED).insert();
        servable(8L);

        assertThat(runner.findQuestionById(unlinked).isPresent(), is(false));
    }

    @Test
    void findByIdReturnsEmptyWhenNoQuestionHasThatId() {
        servable(7L);
        servable(8L);

        assertThat(runner.findQuestionById(9L).isPresent(), is(false));
    }

    // --- exists: same gate, but Micronaut Data translates exists() differently from findOne(), so it is
    // pinned separately against the DB rather than assumed to agree with findById. ---

    @Test
    void doesQuestionExistIsTrueForAnApprovedLinkedQuestion() {
        servable(7L);
        servable(8L);

        assertThat(runner.doesQuestionExistForId(7L), is(true));
    }

    @Test
    void doesQuestionExistIsFalseWhenTheQuestionIsNotApproved() {
        long rejected = 7L;
        data.question(rejected).status(QuestionStatus.REJECTED).insert();
        data.link(rejected, CONCEPT_ID).insert();
        servable(8L);

        assertThat(runner.doesQuestionExistForId(rejected), is(false));
    }

    @Test
    void doesQuestionExistIsFalseWhenTheQuestionHasNoConceptLink() {
        long unlinked = 7L;
        data.question(unlinked).status(QuestionStatus.APPROVED).insert();
        servable(8L);

        assertThat(runner.doesQuestionExistForId(unlinked), is(false));
    }

    @Test
    void doesQuestionExistIsFalseWhenNoQuestionHasThatId() {
        servable(7L);
        servable(8L);

        assertThat(runner.doesQuestionExistForId(9L), is(false));
    }

    // --- helpers ---

    private Page<LinkedQuestion> catalogue() {
        return runner.findQuestionsPagedAndFiltered(null, null, null, FIRST_PAGE);
    }

    // A fully servable question: APPROVED and linked to the given concept (CONCEPT_ID by default). Mirrors the
    // serving policy's two non-negotiables, so a test only spells out the field it means to vary.
    private void servable(long id) {
        servable(id, CONCEPT_ID);
    }

    private void servable(long id, long conceptId) {
        data.question(id).status(QuestionStatus.APPROVED).insert();
        data.link(id, conceptId).insert();
    }

    private static Set<QuestionConceptLink> linksOf(List<LinkedQuestion> content, long questionId) {
        return content.stream()
                .filter(linkedQuestion -> linkedQuestion.question().getId() == questionId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No question with id " + questionId))
                .conceptLinks();
    }

    private static List<Long> ids(Page<LinkedQuestion> page) {
        return page.getContent().stream().map(linkedQuestion -> linkedQuestion.question().getId()).collect(toList());
    }
}
