package com.practiq.service.attempt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static utils.TestReflection.setField;

import com.practiq.persistence.QuestionAttemptEntity;
import com.practiq.persistence.query.attempt.QuestionAttemptQuery;
import com.practiq.persistence.query.attempt.QuestionAttemptQueryRunner;
import com.practiq.persistence.repository.QuestionAttemptRepository;
import com.practiq.service.UserRef;
import com.practiq.service.attempt.dto.request.QuestionAttemptCommand;
import com.practiq.service.attempt.dto.response.QuestionAttempt;
import com.practiq.service.question.QuestionAccessor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// The visibility gate is the whole point of this layer: a question the user may not see must produce an empty
// Optional without the attempt store being touched, so a 404 can't be told apart from "no attempts yet".
@ExtendWith(MockitoExtension.class)
class QuestionAttemptServiceTest {

    @Mock
    private QuestionAccessor questions;

    @Mock
    private QuestionAttemptQueryRunner questionAttemptQueryRunner;

    @Mock
    private QuestionAttemptRepository questionAttemptRepository;

    @InjectMocks
    private QuestionAttemptService questionAttemptService;

    @Test
    void getForQuestionIdReturnsEmptyWhenTheQuestionIsNotVisible() {
        long questionId = 10L;
        when(questions.exists(questionId)).thenReturn(false);

        Optional<List<QuestionAttempt>> result =
                questionAttemptService.getForQuestionId(new UserRef("session-token"), questionId);

        assertThat(result.isPresent(), is(false));
        verifyNoInteractions(questionAttemptQueryRunner);
    }

    @Test
    void getForQuestionIdReturnsThisSessionsMappedAttempts() {
        long questionId = 10L;
        String sessionToken = "session-token";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        when(questions.exists(questionId)).thenReturn(true);
        when(questionAttemptQueryRunner.findAll(new QuestionAttemptQuery(questionId, sessionToken)))
                .thenReturn(List.of(attempt(1L, questionId, sessionToken, "First", createdAt)));

        Optional<List<QuestionAttempt>> result =
                questionAttemptService.getForQuestionId(new UserRef(sessionToken), questionId);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), contains(new QuestionAttempt(1L, questionId, sessionToken, "First", createdAt)));
    }

    @Test
    void getForQuestionIdReturnsAnEmptyListWhenTheVisibleQuestionHasNoAttempts() {
        long questionId = 10L;
        String sessionToken = "session-token";

        when(questions.exists(questionId)).thenReturn(true);
        when(questionAttemptQueryRunner.findAll(new QuestionAttemptQuery(questionId, sessionToken)))
                .thenReturn(List.of());

        Optional<List<QuestionAttempt>> result =
                questionAttemptService.getForQuestionId(new UserRef(sessionToken), questionId);

        // Present but empty, which is what separates "you have not attempted this" from "no such question".
        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(List.of()));
    }

    @Test
    void createReturnsEmptyAndSavesNothingWhenTheQuestionIsNotVisible() {
        long questionId = 10L;
        when(questions.exists(questionId)).thenReturn(false);

        Optional<QuestionAttempt> result = questionAttemptService.create(
                new QuestionAttemptCommand(questionId, new UserRef("session-token"), "An attempt."));

        assertThat(result.isPresent(), is(false));
        verify(questionAttemptRepository, never()).save(any(QuestionAttemptEntity.class));
    }

    @Test
    void createSavesTheAttemptAndReturnsWhatPersistenceAssigned() {
        long questionId = 10L;
        String sessionToken = "session-token";
        String body = "An attempt.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        when(questions.exists(questionId)).thenReturn(true);
        when(questionAttemptRepository.save(any(QuestionAttemptEntity.class))).thenAnswer(invocation -> {
            QuestionAttemptEntity saved = invocation.getArgument(0);
            setField(saved, "id", 99L);
            setField(saved, "createdAt", createdAt);
            return saved;
        });

        Optional<QuestionAttempt> result =
                questionAttemptService.create(new QuestionAttemptCommand(questionId, new UserRef(sessionToken), body));

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), equalTo(new QuestionAttempt(99L, questionId, sessionToken, body, createdAt)));
    }

    private static QuestionAttemptEntity attempt(
            long id, long questionId, String sessionToken, String body, Instant createdAt) {
        QuestionAttemptEntity attempt = new QuestionAttemptEntity(questionId, sessionToken, body);
        setField(attempt, "id", id);
        setField(attempt, "createdAt", createdAt);
        return attempt;
    }
}
