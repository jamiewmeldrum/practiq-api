package com.practiq.persistence.query.attempt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.practiq.persistence.QuestionAttemptEntity;
import com.practiq.persistence.repository.QuestionAttemptRepository;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void findAllReturnsTheRepositoriesResultsUnderTheStableOrder() {
        long questionId = 7L;
        String sessionToken = "session-token";
        List<QuestionAttemptEntity> found = List.of(
                new QuestionAttemptEntity(questionId, sessionToken, "attempt 1"),
                new QuestionAttemptEntity(questionId, sessionToken, "attempt 2"));
        when(questionAttemptRepository.findAll(anySpec(), eq(STABLE_ORDER))).thenReturn(found);

        List<QuestionAttemptEntity> result = runner.findAll(new QuestionAttemptQuery(questionId, sessionToken));

        assertEquals(found, result);
        verify(questionAttemptRepository).findAll(anySpec(), eq(STABLE_ORDER));
    }

    // Typed matcher so overload resolution picks findAll(QuerySpecification, Sort) over its siblings; the runner
    // builds the spec from its real factory, so the test matches on type rather than the instance.
    private static QuerySpecification<QuestionAttemptEntity> anySpec() {
        return any();
    }
}
