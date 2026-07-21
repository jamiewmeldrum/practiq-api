package integration.repository;

import com.practiq.domain.QuestionAttempt;
import com.practiq.domain.query.attempt.QuestionAttemptQuery;
import com.practiq.domain.query.attempt.QuestionAttemptSpecificationFactory;
import com.practiq.repository.QuestionAttemptRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.QuestionTestData;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

@IntegrationTest
class QuestionAttemptSpecificationFactoryIT {

    @Inject
    private QuestionTestData data;

    @Inject
    private QuestionAttemptRepository questionAttemptRepository;

    @Inject
    private QuestionAttemptSpecificationFactory questionAttemptSpecificationFactory;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void forQueryFiltersToTheQuestionIdOnTheQuery() {
        String sessionToken = "session-token";

        long questionId = 1L;
        data.question(questionId).insert();
        long otherQuestionId = 2L;
        data.question(otherQuestionId).insert();

        long attemptId = 10L;
        data.questionAttempt(questionId, sessionToken, "body").id(attemptId).insert();
        long otherQuestionAttemptId = 20L;
        data.questionAttempt(otherQuestionId, sessionToken, "body").id(otherQuestionAttemptId).insert();

        QuestionAttemptQuery query = new QuestionAttemptQuery(questionId, sessionToken);

        // Only the requested question's attempt survives; the other question's attempt (same session) is out.
        assertThat(ids(findAttempts(query)), containsInAnyOrder(attemptId));
    }

    @Test
    void forQueryScopesToTheSessionTokenOnTheQuery() {
        long questionId = 1L;
        data.question(questionId).insert();

        String sessionToken = "session-token";
        long attemptId = 10L;
        data.questionAttempt(questionId, sessionToken, "body").id(attemptId).insert();

        long otherSessionAttemptId = 20L;
        data.questionAttempt(questionId, "other-session", "body").id(otherSessionAttemptId).insert();

        QuestionAttemptQuery query = new QuestionAttemptQuery(questionId, sessionToken);

        // Only this session's attempt survives; another session's attempt on the same question is out.
        assertThat(ids(findAttempts(query)), containsInAnyOrder(attemptId));
    }

    private List<QuestionAttempt> findAttempts(QuestionAttemptQuery query) {
        return questionAttemptRepository.findAll(questionAttemptSpecificationFactory.forQuery(query));
    }

    private static List<Long> ids(List<QuestionAttempt> attempts) {
        return attempts.stream().map(QuestionAttempt::getId).collect(toList());
    }
}
