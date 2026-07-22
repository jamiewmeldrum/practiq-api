package com.practiq.service;

import com.practiq.domain.Question;
import com.practiq.domain.QuestionAttempt;
import com.practiq.domain.QuestionAttempt_;
import com.practiq.domain.query.attempt.QuestionAttemptQueryRunner;
import com.practiq.domain.query.question.StudentQuestionQueryRunner;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.dto.filter.UserRequestFilter;
import com.practiq.dto.response.QuestionAttemptResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
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
    void getQuestionAttemptByIdReturnsTheMappedAttemptsInOrderWhenTheQuestionExists() {
        long questionId = 5L;
        String sessionToken = "session-token";
        UserRequestFilter userRequestFilter = new UserRequestFilter(sessionToken);

        long attemptId1 = 10L;
        QuestionAttempt attempt1 = new QuestionAttempt(questionId, sessionToken, "attempt1");
        setField(attempt1, "id", attemptId1);

        long attemptId2 = 20L;
        QuestionAttempt attempt2 = new QuestionAttempt(questionId, sessionToken, "attempt2");
        setField(attempt2, "id", attemptId2);

        when(questionQueryRunner.doesQuestionExistForId(questionId)).thenReturn(true);
        when(questionAttemptQueryRunner.getQuestionAttempts(userRequestFilter, questionId)).thenReturn(List.of(attempt1, attempt2));

        Optional<List<QuestionAttemptResponse>> attempts = questionAttemptService.getForQuestionId(userRequestFilter, questionId);

        // The service maps each attempt and preserves the runner's order; the per-field mapping is
        // QuestionAttemptResponseMapperTest's contract, so assert identity and order here, not every field.
        assertThat(attempts.isPresent(), is(true));
        assertThat(attempts.get().stream().map(QuestionAttemptResponse::getId).toList(), contains(attemptId1, attemptId2));

        verify(questionQueryRunner).doesQuestionExistForId(questionId);
        verify(questionAttemptQueryRunner).getQuestionAttempts(userRequestFilter, questionId);
    }
}
