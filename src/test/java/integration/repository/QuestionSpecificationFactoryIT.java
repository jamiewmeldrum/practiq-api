package integration.repository;

import com.practiq.domain.Question;
import com.practiq.domain.query.question.QuestionQuery;
import com.practiq.domain.query.question.QuestionSpecificationFactory;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import com.practiq.repository.QuestionRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.QuestionTestData;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

@IntegrationTest
class QuestionSpecificationFactoryIT {

    @Inject
    private QuestionTestData data;

    @Inject
    private QuestionRepository questionRepository;

    @Inject
    private QuestionSpecificationFactory questionSpecificationFactory;

    @BeforeEach
    void setUp() {
        data.clear();
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

        // Each status returns only its own rows — proving the filter is driven by the query's status
        // value, not a hard-coded one.
        QuestionQuery approvedQuery = QuestionQuery.builder()
                .status(QuestionStatus.APPROVED)
                .build();
        assertThat(ids(findQuestions(approvedQuery)), containsInAnyOrder(approvedOneId, approvedTwoId));

        QuestionQuery pendingQuery = QuestionQuery.builder()
                .status(QuestionStatus.PENDING)
                .build();
        assertThat(ids(findQuestions(pendingQuery)), containsInAnyOrder(pendingId));

        QuestionQuery rejectedQuery = QuestionQuery.builder()
                .status(QuestionStatus.REJECTED)
                .build();
        assertThat(ids(findQuestions(rejectedQuery)), containsInAnyOrder(rejectedId));
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

        // Only the two requested types come back; the MCQ row is excluded.
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

        // The stored integer difficulty (1/3/5) is matched against the requested enum values via the
        // attribute converter: the MEDIUM (3) row is excluded, TRIVIAL (1) and VERY_HARD (5) survive.
        assertThat(ids(findQuestions(query)), containsInAnyOrder(trivialId, veryHardId));
    }

    @Test
    void forQueryFiltersToTheConceptIdOnTheQuery() {
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

        QuestionQuery query = QuestionQuery.builder()
                .conceptId(conceptA)
                .build();

        // Only questions linked to concept A survive: the A-only question and the synoptic (A+B) one.
        // The B-only question is excluded. Critically the synoptic question appears exactly once despite
        // matching two of concept A's link rows — a join would have returned it twice; EXISTS doesn't
        // multiply rows, so the page count stays honest. (containsInAnyOrder also asserts the exact size,
        // so a duplicate would fail this.)
        assertThat(ids(findQuestions(query)), containsInAnyOrder(linkedToA, synoptic));
    }

    @Test
    void forQueryFiltersToTheQuestionIdOnTheQuery() {
        long idA = 1L;
        long idB = 2L;
        data.question(idA).insert();
        data.question(idB).insert();

        QuestionQuery query = QuestionQuery.builder()
                .questionId(idB)
                .build();

        assertThat(ids(findQuestions(query)), contains(idB));
    }

    @Test
    void requiresConceptLinkFiltersUnlinkedQuestions() {
        long linkedId = 1L;
        long unlinkedId = 2L;
        data.question(linkedId).insert();
        data.question(unlinkedId).insert();

        long concept = 100L;
        data.concept(concept).insert();

        data.link(linkedId, concept).insert();

        //Explicit test to ensure behavior actually flips
        QuestionQuery query = QuestionQuery.builder()
                .requiresConceptLink(false)
                .build();

        assertThat(ids(findQuestions(query)), containsInAnyOrder(linkedId, unlinkedId));

        query = QuestionQuery.builder()
                .requiresConceptLink(true)
                .build();

        assertThat(ids(findQuestions(query)), containsInAnyOrder(linkedId));
    }

    @Test
    void forQueryAppliesAllFiltersConjunctively() {
        long matches = 1L;
        long wrongQuestionId = 2L;
        long wrongType = 3L;
        long wrongDifficulty = 4L;
        long wrongStatus = 5L;
        long unlinked = 6L;
        long wrongLink = 7L;

        data.question(matches).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.question(wrongQuestionId).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.question(wrongType).status(QuestionStatus.APPROVED).type(QuestionType.MCQ).difficulty(QuestionDifficulty.EASY).insert();
        data.question(wrongDifficulty).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.HARD).insert();
        data.question(wrongStatus).status(QuestionStatus.PENDING).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.question(unlinked).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.question(wrongLink).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();

        long conceptA = 100L;
        long conceptB = 101L;

        data.concept(conceptA).insert();
        data.concept(conceptB).insert();

        data.link(matches, conceptA).insert();
        data.link(wrongType, conceptA).insert();
        data.link(wrongDifficulty, conceptA).insert();
        data.link(wrongStatus, conceptA).insert();
        data.link(wrongLink, conceptB).insert();

        QuestionQuery query = QuestionQuery.builder()
                .types(List.of(QuestionType.SHORT_ANSWER))
                .difficulties(List.of(QuestionDifficulty.EASY))
                .status(QuestionStatus.APPROVED)
                .conceptId(conceptA)
                .questionId(matches)
                .requiresConceptLink(true)
                .build();

        assertThat(ids(findQuestions(query)), containsInAnyOrder(matches));
    }

    @Test
    void forQueryWithoutFiltersReturnsAll() {
        long base = 1L;
        long differentQuestionId = 2L;
        long differentType = 3L;
        long differentDifficulty = 4L;
        long differentStatus = 5L;
        long unlinked = 6L;
        long differentLink = 7L;

        data.question(base).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.question(differentQuestionId).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.question(differentType).status(QuestionStatus.APPROVED).type(QuestionType.MCQ).difficulty(QuestionDifficulty.EASY).insert();
        data.question(differentDifficulty).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.HARD).insert();
        data.question(differentStatus).status(QuestionStatus.PENDING).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.question(unlinked).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();
        data.question(differentLink).status(QuestionStatus.APPROVED).type(QuestionType.SHORT_ANSWER).difficulty(QuestionDifficulty.EASY).insert();

        long conceptA = 100L;
        long conceptB = 101L;

        data.concept(conceptA).insert();
        data.concept(conceptB).insert();

        data.link(base, conceptA).insert();
        data.link(differentType, conceptA).insert();
        data.link(differentDifficulty, conceptA).insert();
        data.link(differentStatus, conceptA).insert();
        data.link(differentLink, conceptB).insert();

        QuestionQuery query = QuestionQuery.builder().build();

        assertThat(ids(findQuestions(query)),
                containsInAnyOrder(
                        base,
                        differentQuestionId,
                        differentType,
                        differentDifficulty,
                        differentStatus,
                        unlinked,
                        differentLink
                ));
    }

    private List<Question> findQuestions(QuestionQuery query) {
        return questionRepository.findAll(questionSpecificationFactory.forQuery(query));
    }

    private static List<Long> ids(List<Question> questions) {
        return questions.stream().map(Question::getId).collect(toList());
    }
}
