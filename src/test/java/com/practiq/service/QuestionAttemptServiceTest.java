package com.practiq.service;

import com.practiq.domain.QuestionAttempt;
import com.practiq.domain.query.attempt.QuestionAttemptQueryRunner;
import com.practiq.domain.query.question.StudentQuestionQueryRunner;
import com.practiq.dto.filter.UserRequestFilter;
import com.practiq.dto.request.QuestionAttemptRequest;
import com.practiq.dto.response.QuestionAttemptResponse;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static utils.TestReflection.setField;

@ExtendWith(MockitoExtension.class)
class QuestionAttemptServiceTest {

    @Mock
    private StudentQuestionQueryRunner questionQueryRunner;

    @Mock
    private QuestionAttemptQueryRunner questionAttemptQueryRunner;

    @InjectMocks
    private QuestionAttemptService questionAttemptService;

    @Test
    void getQuestionAttemptByIdReturnsOptionalEmptyIfNoQuestionForId() {
        long questionId = 5L;
        String sessionToken = "session-token";
        UserRequestFilter userRequestFilter = new UserRequestFilter(sessionToken);

        when(questionQueryRunner.doesQuestionExistForId(questionId)).thenReturn(false);

        Optional<List<QuestionAttemptResponse>> attempts = questionAttemptService.getForQuestionId(userRequestFilter, questionId);

        assertThat(attempts.isPresent(), is(false));

        verify(questionQueryRunner).doesQuestionExistForId(questionId);
    }

    @Test
    void getQuestionAttemptByIdReturnsOptionalOfEmptyListIfNoAttemptsForQuestion() {
        long questionId = 5L;
        String sessionToken = "session-token";
        UserRequestFilter userRequestFilter = new UserRequestFilter(sessionToken);

        when(questionQueryRunner.doesQuestionExistForId(questionId)).thenReturn(true);
        when(questionAttemptQueryRunner.getQuestionAttempts(userRequestFilter, questionId)).thenReturn(List.of());

        Optional<List<QuestionAttemptResponse>> attempts = questionAttemptService.getForQuestionId(userRequestFilter, questionId);

        assertThat(attempts.isPresent(), is(true));
        assertThat(attempts.get(), empty());

        verify(questionQueryRunner).doesQuestionExistForId(questionId);
        verify(questionAttemptQueryRunner).getQuestionAttempts(userRequestFilter, questionId);
    }

    @Test
    void getQuestionAttemptByIdReturnsOptionalListIfAttemptsForQuestion() {
        long questionId = 5L;
        String sessionToken = "session-token";
        UserRequestFilter userRequestFilter = new UserRequestFilter(sessionToken);

        long attemptId1 = 10L;
        String attemptBody1 = "attempt1";
        Instant createdAt1 = Instant.parse("2026-01-01T00:00:00Z");
        QuestionAttempt attempt1 =  new QuestionAttempt(questionId, sessionToken, attemptBody1);
        setField(attempt1, "id", attemptId1);
        setField(attempt1, "createdAt", createdAt1);

        long attemptId2 = 20L;
        String attemptBody2 = "attempt2";
        Instant createdAt2 = Instant.parse("2026-01-02T00:00:00Z");
        QuestionAttempt attempt2 =  new QuestionAttempt(questionId, sessionToken, attemptBody2);
        setField(attempt2, "id", attemptId2);
        setField(attempt2, "createdAt", createdAt2);

        when(questionQueryRunner.doesQuestionExistForId(questionId)).thenReturn(true);
        when(questionAttemptQueryRunner.getQuestionAttempts(userRequestFilter, questionId)).thenReturn(List.of(attempt1, attempt2));

        Optional<List<QuestionAttemptResponse>> attempts = questionAttemptService.getForQuestionId(userRequestFilter, questionId);

        // The full ordered response list is the service's observable outcome — asserted independently of how it
        // is produced, so a mis-wired or broken mapping fails here even though the mapper's own test stays green.
        assertThat(attempts.isPresent(), is(true));
        assertThat(attempts.get().size(), is(2));

        QuestionAttemptResponse attemptResponse1 = attempts.get().get(0);
        assertThat(attemptResponse1.getId(), equalTo(attemptId1));
        assertThat(attemptResponse1.getQuestionId(), equalTo(questionId));
        assertThat(attemptResponse1.getBody(), equalTo(attemptBody1));
        assertThat(attemptResponse1.getCreatedAt(), equalTo(createdAt1));

        QuestionAttemptResponse attemptResponse2 = attempts.get().get(1);
        assertThat(attemptResponse2.getId(), equalTo(attemptId2));
        assertThat(attemptResponse2.getQuestionId(), equalTo(questionId));
        assertThat(attemptResponse2.getBody(), equalTo(attemptBody2));
        assertThat(attemptResponse2.getCreatedAt(), equalTo(createdAt2));

        verify(questionQueryRunner).doesQuestionExistForId(questionId);
        verify(questionAttemptQueryRunner).getQuestionAttempts(userRequestFilter, questionId);
    }

    @Test
    void postQuestionAttemptByIdReturnsOptionalEmptyIfNoQuestionForId() {
        long questionId = 5L;
        String sessionToken = "session-token";
        String body = "attempt";
        QuestionAttemptRequest request = new QuestionAttemptRequest(body);

        when(questionQueryRunner.doesQuestionExistForId(questionId)).thenReturn(false);

        Optional<QuestionAttemptResponse> attempt = questionAttemptService.postForQuestionId(sessionToken, request, questionId);

        assertThat(attempt.isPresent(), is(false));

        verify(questionQueryRunner).doesQuestionExistForId(questionId);
    }

    @Test
    void postQuestionAttemptReturnsUpdatedEntity() {
        long questionId = 5L;
        String sessionToken = "session-token";
        String body = "attempt";
        QuestionAttemptRequest request = new QuestionAttemptRequest(body);

        QuestionAttempt incomingAttempt = new QuestionAttempt(questionId, sessionToken, body);
        long attemptId = 20L;
        Instant createdAt = Instant.parse("2026-01-03T00:00:00Z");
        QuestionAttempt attemptDB = new QuestionAttempt(questionId, sessionToken, body);
        setField(attemptDB, "createdAt", createdAt);
        setField(attemptDB, "id", attemptId);

        when(questionQueryRunner.doesQuestionExistForId(questionId)).thenReturn(true);
        when(questionAttemptQueryRunner.postQuestionAttempt(incomingAttempt)).thenReturn(attemptDB);

        Optional<QuestionAttemptResponse> attempt = questionAttemptService.postForQuestionId(sessionToken, request, questionId);

        assertThat(attempt.isPresent(), is(true));

        QuestionAttemptResponse response = attempt.get();
        assertThat(response.getId(), is(attemptId));
        assertThat(response.getQuestionId(), is(questionId));
        assertThat(response.getBody(), is(body));
        assertThat(response.getCreatedAt(), is(createdAt));

        verify(questionQueryRunner).doesQuestionExistForId(questionId);
        verify(questionAttemptQueryRunner).postQuestionAttempt(incomingAttempt);
    }
}
