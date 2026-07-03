package com.practiq.domain.query;

import com.practiq.domain.Question;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.repository.QuestionRepository;
import com.practiq.test.QuestionTestData;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

// A QuerySpecification is an opaque lambda — the only faithful way to verify what it adds to the
// query (the status filter, the left-join fetch) is to execute it against a real database and
// observe the rows. transactional = false is deliberate: the test method runs outside a session,
// so a returned entity's conceptLinks are only accessible if the spec actually FETCHED them.
@MicronautTest(transactional = false)
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
    void fromFiltersToTheStatusOnTheQuery() {
        long approvedOneId = 1L;
        long approvedTwoId = 2L;
        long pendingId = 3L;
        long rejectedId = 4L;
        data.question(approvedOneId).status(QuestionStatus.APPROVED).insert();
        data.question(approvedTwoId).status(QuestionStatus.APPROVED).insert();
        data.question(pendingId).status(QuestionStatus.PENDING).insert();
        data.question(rejectedId).status(QuestionStatus.REJECTED).insert();

        // Each status returns only its own rows — proving the filter is driven by the query's
        // status value, not a hard-coded one.
        QuestionQuery approvedQuery = new QuestionQuery(QuestionStatus.APPROVED);
        List<Question> approvedQuestions = findQuestions(approvedQuery);
        assertThat(ids(approvedQuestions), containsInAnyOrder(approvedOneId, approvedTwoId));

        QuestionQuery pendingQuery = new QuestionQuery(QuestionStatus.PENDING);
        List<Question> pendingQuestion = findQuestions(pendingQuery);
        assertThat(ids(pendingQuestion), containsInAnyOrder(pendingId));

        QuestionQuery rejectedQuery = new QuestionQuery(QuestionStatus.REJECTED);
        List<Question> rejectedQuestions = findQuestions(rejectedQuery);
        assertThat(ids(rejectedQuestions), containsInAnyOrder(rejectedId));
    }

    @Test
    void fromLeftJoinsAndFetchesConceptLinks() {
        long diffractionId = 10L;
        long accelerationId = 11L;
        data.concept(diffractionId, "Diffraction", "The spreading of waves through a gap or around an obstacle.");
        data.concept(accelerationId, "Acceleration", "How the velocity of an object changes over time.");

        long unlinkedId = 1L;
        long linkedId = 2L;
        data.question(unlinkedId).status(QuestionStatus.APPROVED).insert();
        data.question(linkedId).status(QuestionStatus.APPROVED).insert();
        data.link(linkedId, diffractionId);
        data.link(linkedId, accelerationId);

        QuestionQuery approvedQuery = new QuestionQuery(QuestionStatus.APPROVED);
        List<Question> results = findQuestions(approvedQuery);

        // LEFT join: the question with no concept links is still returned, not dropped by the join.
        assertThat(ids(results), containsInAnyOrder(unlinkedId, linkedId));
        // FETCH: the links are populated, readable here despite the session having closed.
        assertThat(conceptIdsOf(results, unlinkedId), is(empty()));
        assertThat(conceptIdsOf(results, linkedId), containsInAnyOrder(diffractionId, accelerationId));
    }

    private List<Question> findQuestions(QuestionQuery query) {
        return questionRepository.findAll(questionSpecificationFactory.from(query));
    }

    private List<Long> ids(List<Question> questions) {
        return questions
                .stream()
                .map(Question::getId)
                .collect(toList());
    }

    private Set<Long> conceptIdsOf(List<Question> questions, long questionId) {
        return questions.stream()
                .filter(question -> question.getId() == questionId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No question with id " + questionId))
                .getConceptLinks().stream()
                .map(link -> link.getId().getConceptId())
                .collect(toSet());
    }
}
