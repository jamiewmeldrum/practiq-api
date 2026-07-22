package integration.repository;

import com.practiq.domain.QuestionAttempt;
import com.practiq.domain.query.attempt.QuestionAttemptQuery;
import com.practiq.domain.query.attempt.QuestionAttemptSpecificationFactory;
import com.practiq.repository.QuestionAttemptRepository;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.QuestionTestData;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

// How attempt queries behave against a real database: the question-id predicate and the session-token restriction
// the specification carries, plus the stable newest-first order findAll applies. The spec is part of the
// repository's DB interaction, so its filtering is pinned here rather than in a separate spec-factory IT.
@IntegrationTest
public class QuestionAttemptRepositoryIT {

    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

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
    void forQueryReturnsOnlyTheGivenQuestionsAttempts() {
        String sessionToken = "session-token";
        long questionId = 1L;
        long otherQuestionId = 2L;
        data.question(questionId).insert();
        data.question(otherQuestionId).insert();

        long attemptId = 10L;
        long otherQuestionsAttemptId = 20L;
        data.questionAttempt(questionId, sessionToken, "body").id(attemptId).insert();
        data.questionAttempt(otherQuestionId, sessionToken, "body").id(otherQuestionsAttemptId).insert();

        QuestionAttemptQuery query = new QuestionAttemptQuery(questionId, sessionToken);

        // Same session, different question: the other question's attempt is out.
        assertThat(ids(findAttempts(query)), containsInAnyOrder(attemptId));
    }

    @Test
    void forQueryScopesToTheSessionToken() {
        long questionId = 1L;
        data.question(questionId).insert();

        String sessionToken = "session-token";
        long attemptId = 10L;
        long otherSessionAttemptId = 20L;
        data.questionAttempt(questionId, sessionToken, "body").id(attemptId).insert();
        data.questionAttempt(questionId, "other-session", "body").id(otherSessionAttemptId).insert();

        QuestionAttemptQuery query = new QuestionAttemptQuery(questionId, sessionToken);

        // Same question, different session: another session's attempt is never returned.
        assertThat(ids(findAttempts(query)), containsInAnyOrder(attemptId));
    }

    @Test
    void findAllAppliesTheStableNewestThenIdOrder() {
        long questionId = 7L;
        data.question(questionId).insert();

        String sessionToken = "sessionToken";
        OffsetDateTime day1 = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime day2 = OffsetDateTime.parse("2026-01-02T00:00:00Z");

        long day1LowId = 1L;
        long day1HighId = 2L;
        long day2Attempt = 3L;
        data.questionAttempt(questionId, sessionToken, "body").id(day1LowId).createdAt(day1).insert();
        data.questionAttempt(questionId, sessionToken, "body").id(day1HighId).createdAt(day1).insert();
        data.questionAttempt(questionId, sessionToken, "body").id(day2Attempt).createdAt(day2).insert();

        QuestionAttemptQuery query = new QuestionAttemptQuery(questionId, sessionToken);
        QuerySpecification<QuestionAttempt> spec = questionAttemptSpecificationFactory.forQuery(query);

        List<QuestionAttempt> attempts = questionAttemptRepository.findAll(spec, STABLE_ORDER);

        // Newest created_at first; the day1 tie is broken by ascending id.
        assertThat(ids(attempts), contains(day2Attempt, day1LowId, day1HighId));
    }

    @Test
    void savingAttemptSetsAndReturnsDBDelegatedFields() {
        long questionId = 5L;
        data.question(questionId).insert();

        String sessionToken = "session-token";
        String body = "attempt";

        QuestionAttempt incomingAttempt = new QuestionAttempt(questionId, sessionToken, body);
        QuestionAttempt attempt = questionAttemptRepository.save(incomingAttempt);

        assertThat(attempt.getId(), instanceOf(Long.class));
        assertThat(attempt.getQuestionId(), is(questionId));
        assertThat(attempt.getSessionToken(), is(sessionToken));
        assertThat(attempt.getBody(), is(body));
        assertThat(attempt.getCreatedAt().toString(), matchesPattern(data.getInstantPattern()));
    }

    @Test
    void cannotSaveAttemptWithEmptyBody() {
        long questionId = 5L;
        data.question(questionId).insert();

        String sessionToken = "session-token";

        //Check 1 char saves
        QuestionAttempt validAttempt = new QuestionAttempt(questionId, sessionToken, "a");
        QuestionAttempt attempt = questionAttemptRepository.save(validAttempt);
        assertThat(attempt.getId(), instanceOf(Long.class));

        //Check empty doesn't save
        QuestionAttempt invalidAttempt = new QuestionAttempt(questionId, sessionToken, "");
        ConstraintViolationException thrown = assertThrows(ConstraintViolationException.class, () ->
                questionAttemptRepository.save(invalidAttempt)
        );

        Set<ConstraintViolation<?>> constraintViolations = thrown.getConstraintViolations();
        assertThat(constraintViolations.size(), is(1));
        String message = constraintViolations.stream().findFirst().get().getMessage();
        assertThat(message, equalTo("size must be between 1 and 100000"));
    }

    @Test
    void cannotSaveAttemptWithTooLargeBody() {
        long questionId = 5L;
        data.question(questionId).insert();

        String sessionToken = "session-token";

        //Check 100000 char saves
        String validBody = RandomStringUtils.insecure().nextAlphanumeric(100000);
        QuestionAttempt validAttempt = new QuestionAttempt(questionId, sessionToken, validBody);
        QuestionAttempt attempt = questionAttemptRepository.save(validAttempt);
        assertThat(attempt.getId(), instanceOf(Long.class));

        //Check 100001 chars doesn't save
        String body = RandomStringUtils.insecure().nextAlphanumeric(100001);
        QuestionAttempt invalidAttempt = new QuestionAttempt(questionId, sessionToken, body);
        ConstraintViolationException thrown = assertThrows(ConstraintViolationException.class, () ->
                questionAttemptRepository.save(invalidAttempt)
        );

        Set<ConstraintViolation<?>> constraintViolations = thrown.getConstraintViolations();
        assertThat(constraintViolations.size(), is(1));
        String message = constraintViolations.stream().findFirst().get().getMessage();
        assertThat(message, equalTo("size must be between 1 and 100000"));
    }

    private List<QuestionAttempt> findAttempts(QuestionAttemptQuery query) {
        return questionAttemptRepository.findAll(questionAttemptSpecificationFactory.forQuery(query));
    }

    private static List<Long> ids(List<QuestionAttempt> attempts) {
        return attempts.stream().map(QuestionAttempt::getId).collect(toList());
    }
}
