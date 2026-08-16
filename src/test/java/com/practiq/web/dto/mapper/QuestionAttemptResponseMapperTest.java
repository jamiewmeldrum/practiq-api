package com.practiq.web.dto.mapper;

import static com.practiq.web.dto.mapper.QuestionAttemptResponseMapper.toQuestionAttemptResponse;
import static com.practiq.web.dto.mapper.QuestionAttemptResponseMapper.toQuestionAttemptResponses;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.practiq.service.attempt.dto.response.QuestionAttempt;
import com.practiq.web.dto.response.QuestionAttemptResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionAttemptResponseMapperTest {

    @Test
    void questionAttemptMapsToQuestionAttemptResponse() {
        long id = 1L;
        long questionId = 10L;
        String body = "Because the wave bends around the edge of the gap.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        // The session token the caller supplied: the response has no field for it, so it cannot be echoed
        // back into bodies, logs or caches.
        QuestionAttempt questionAttempt = new QuestionAttempt(id, questionId, "a-session-token", body, createdAt);

        QuestionAttemptResponse response = toQuestionAttemptResponse(questionAttempt);

        assertThat(response.id(), equalTo(id));
        assertThat(response.questionId(), equalTo(questionId));
        assertThat(response.body(), equalTo(body));
        assertThat(response.createdAt(), equalTo(createdAt));
    }

    @Test
    void questionAttemptsMapToQuestionAttemptResponsesInOrder() {
        QuestionAttempt first = new QuestionAttempt(1L, 10L, "token", "First", Instant.parse("2026-01-02T00:00:00Z"));
        QuestionAttempt second = new QuestionAttempt(2L, 10L, "token", "Second", Instant.parse("2026-01-01T00:00:00Z"));

        List<QuestionAttemptResponse> responses = toQuestionAttemptResponses(List.of(first, second));

        assertThat(responses.stream().map(QuestionAttemptResponse::body).toList(), contains("First", "Second"));
    }
}
