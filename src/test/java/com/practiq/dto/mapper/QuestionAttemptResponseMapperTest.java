package com.practiq.dto.mapper;

import com.practiq.domain.QuestionAttempt;
import com.practiq.dto.response.QuestionAttemptResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.practiq.dto.mapper.QuestionAttemptResponseMapper.toQuestionAttemptResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static utils.TestReflection.setField;

class QuestionAttemptResponseMapperTest {

    @Test
    void toQuestionAttemptResponseMapsExpectedFields() {
        long questionId = 5L;
        String sessionToken = "session-token";

        long attemptId = 10L;
        String attemptBody = "attempt";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        QuestionAttempt attempt =  new QuestionAttempt(questionId, sessionToken, attemptBody);
        setField(attempt, "id", attemptId);
        setField(attempt, "createdAt", createdAt);

        QuestionAttemptResponse attemptResponse = toQuestionAttemptResponse(attempt);

        assertThat(attemptResponse.getId(), equalTo(attemptId));
        assertThat(attemptResponse.getQuestionId(), equalTo(questionId));
        assertThat(attemptResponse.getBody(), equalTo(attemptBody));
        assertThat(attemptResponse.getCreatedAt(), equalTo(createdAt));
    }

}