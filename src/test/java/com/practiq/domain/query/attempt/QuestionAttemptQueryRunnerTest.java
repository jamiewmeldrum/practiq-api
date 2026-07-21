package com.practiq.domain.query.attempt;

import com.practiq.domain.QuestionAttempt;
import com.practiq.dto.filter.UserRequestFilter;
import com.practiq.repository.QuestionAttemptRepository;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Proves the runner's mechanics in isolation: it builds the query from the request's session token and the
// question id, asks the factory for a spec from exactly that query, and runs it against the repository under
// the stable (created_at desc, id asc) order.
@ExtendWith(MockitoExtension.class)
class QuestionAttemptQueryRunnerTest {

    // Sentinel handed back by the mocked factory so we can assert this exact instance reaches the repo.
    private static final QuerySpecification<QuestionAttempt> SPEC = (root, query, cb) -> null;
    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    @Mock
    private QuestionAttemptRepository questionAttemptRepository;

    @Mock
    private QuestionAttemptSpecificationFactory questionAttemptSpecificationFactory;

    @InjectMocks
    private QuestionAttemptQueryRunner runner;

    @Test
    void getQuestionAttemptsRunsTheSessionQueryOnAStableSortedList() {
        long questionId = 7L;
        String sessionToken = "session-token";

        QuestionAttemptQuery query = new QuestionAttemptQuery(questionId, sessionToken);
        List<QuestionAttempt> found = List.of(
                new QuestionAttempt(questionId, sessionToken, "attempt 1"),
                new QuestionAttempt(questionId, sessionToken, "attempt 2")
        );
        when(questionAttemptSpecificationFactory.forQuery(query)).thenReturn(SPEC);
        when(questionAttemptRepository.findAll(SPEC, STABLE_ORDER)).thenReturn(found);

        List<QuestionAttempt> result = runner.getQuestionAttempts(new UserRequestFilter(sessionToken), questionId);

        assertEquals(found, result);
        verify(questionAttemptSpecificationFactory).forQuery(query);
        verify(questionAttemptRepository).findAll(SPEC, STABLE_ORDER);
    }
}
