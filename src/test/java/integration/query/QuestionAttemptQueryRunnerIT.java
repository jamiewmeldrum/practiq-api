package integration.query;

import com.practiq.domain.QuestionAttempt;
import com.practiq.domain.query.attempt.QuestionAttemptQueryRunner;
import com.practiq.dto.filter.UserRequestFilter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.QuestionTestData;

import java.time.OffsetDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

// The attempt read path as one unit against a real database, mirroring StudentQuestionQueryRunnerIT: the
// runner drives the QuestionAttemptSpecificationFactory (question-id predicate + session-token restriction)
// and the repository under the stable newest-first order. The session scoping in particular is a security
// invariant — a session must never be served another session's attempts — and it is proven here as observable
// behaviour rather than by asserting the factory happens to declare a SessionTokenRestriction instance.
//
// Replaces QuestionAttemptQueryRunnerTest, QuestionAttemptSpecificationFactoryTest,
// QuestionAttemptSpecificationFactoryIT, and the ordering test from QuestionAttemptRepositoryIT.
@IntegrationTest
class QuestionAttemptQueryRunnerIT {

    private static final String SESSION_TOKEN = "session-token";

    @Inject
    private QuestionAttemptQueryRunner runner;

    @Inject
    private QuestionTestData data;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void getQuestionAttemptsReturnsOnlyThisQuestionsAttempts() {
        long questionId = 1L;
        long otherQuestionId = 2L;
        data.question(questionId).insert();
        data.question(otherQuestionId).insert();

        long wanted = 10L;
        long otherQuestionsAttempt = 20L;
        data.questionAttempt(questionId, SESSION_TOKEN, "body").id(wanted).insert();
        data.questionAttempt(otherQuestionId, SESSION_TOKEN, "body").id(otherQuestionsAttempt).insert();

        // Same session, different question: the other question's attempt is out.
        assertThat(ids(getAttempts(questionId)), containsInAnyOrder(wanted));
    }

    @Test
    void getQuestionAttemptsScopesToTheRequestingSession() {
        long questionId = 1L;
        data.question(questionId).insert();

        long ours = 10L;
        long anotherSessions = 20L;
        data.questionAttempt(questionId, SESSION_TOKEN, "body").id(ours).insert();
        data.questionAttempt(questionId, "another-session", "body").id(anotherSessions).insert();

        // Same question, different session: another session's attempt is never served to us.
        assertThat(ids(getAttempts(questionId)), containsInAnyOrder(ours));
    }

    @Test
    void getQuestionAttemptsReturnsNewestCreatedAtThenIdOrder() {
        long questionId = 1L;
        data.question(questionId).insert();

        OffsetDateTime day1 = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime day2 = OffsetDateTime.parse("2026-01-02T00:00:00Z");

        long day1LowId = 1L;
        long day1HighId = 2L;
        long day2Attempt = 3L;
        data.questionAttempt(questionId, SESSION_TOKEN, "body").id(day1LowId).createdAt(day1).insert();
        data.questionAttempt(questionId, SESSION_TOKEN, "body").id(day1HighId).createdAt(day1).insert();
        data.questionAttempt(questionId, SESSION_TOKEN, "body").id(day2Attempt).createdAt(day2).insert();

        // Newest created_at first; the day1 tie is broken by ascending id.
        assertThat(ids(getAttempts(questionId)), contains(day2Attempt, day1LowId, day1HighId));
    }

    private List<QuestionAttempt> getAttempts(long questionId) {
        return runner.getQuestionAttempts(new UserRequestFilter(SESSION_TOKEN), questionId);
    }

    private static List<Long> ids(List<QuestionAttempt> attempts) {
        return attempts.stream().map(QuestionAttempt::getId).collect(toList());
    }
}
