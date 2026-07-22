package com.practiq.domain.query.attempt;

import com.practiq.domain.QuestionAttempt;
import com.practiq.dto.filter.UserRequestFilter;
import com.practiq.repository.QuestionAttemptRepository;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static utils.TestReflection.setField;

// Runner as the unit entry point: the real QuestionAttemptSpecificationFactory (question-id predicate + session
// restriction) is exercised through it, and only the repository is mocked. What the query FILTERS on — question id
// and session token — is proven against the DB in integration.repository.QuestionAttemptRepositoryIT; here we prove
// the runner delegates under the stable newest-first order and returns what the repository gives back.
@ExtendWith(MockitoExtension.class)
class QuestionAttemptQueryRunnerTest {

    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    @Mock
    private QuestionAttemptRepository questionAttemptRepository;

    private QuestionAttemptQueryRunner runner;

    @BeforeEach
    void setUp() {
        runner = new QuestionAttemptQueryRunner(questionAttemptRepository, new QuestionAttemptSpecificationFactory());
    }

    @Test
    void getQuestionAttemptsReturnsTheRepositoriesResultsUnderTheStableOrder() {
        long questionId = 7L;
        String sessionToken = "session-token";
        List<QuestionAttempt> found = List.of(
                new QuestionAttempt(questionId, sessionToken, "attempt 1"),
                new QuestionAttempt(questionId, sessionToken, "attempt 2"));
        when(questionAttemptRepository.findAll(anySpec(), eq(STABLE_ORDER))).thenReturn(found);

        List<QuestionAttempt> result = runner.getQuestionAttempts(new UserRequestFilter(sessionToken), questionId);

        assertEquals(found, result);
        verify(questionAttemptRepository).findAll(anySpec(), eq(STABLE_ORDER));
    }

    @Test
    void createQuestionAttemptUsesSavedEntity() {
        long questionId = 5L;
        String sessionToken = "session-token";
        String body = "attempt";

        QuestionAttempt incomingAttempt = new QuestionAttempt(questionId, sessionToken, body);
        long attemptId = 20L;
        Instant createdAt = Instant.parse("2026-01-03T00:00:00Z");
        QuestionAttempt attemptDB = new QuestionAttempt(questionId, sessionToken, body);
        setField(attemptDB, "createdAt", createdAt);
        setField(attemptDB, "id", attemptId);

        when(questionAttemptRepository.save(incomingAttempt)).thenReturn(attemptDB);

        QuestionAttempt attempt = runner.createQuestionAttempt(incomingAttempt);

        assertThat(attempt, is(attemptDB));

        verify(questionAttemptRepository).save(incomingAttempt);
    }

    // Typed matcher so overload resolution picks findAll(QuerySpecification, Sort) over its siblings; the runner
    // builds the spec from its real factory, so the test matches on type rather than the instance.
    private static QuerySpecification<QuestionAttempt> anySpec() {
        return any();
    }
}
