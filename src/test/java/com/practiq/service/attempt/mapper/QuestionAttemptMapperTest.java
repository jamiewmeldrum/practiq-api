package com.practiq.service.attempt.mapper;

import static com.practiq.service.attempt.mapper.QuestionAttemptMapper.toQuestionAttempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static utils.TestReflection.setField;

import com.practiq.persistence.QuestionAttemptEntity;
import com.practiq.service.attempt.dto.response.QuestionAttempt;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class QuestionAttemptMapperTest {

    @Test
    void questionAttemptEntityMapsToQuestionAttempt() {
        long id = 1L;
        long questionId = 10L;
        String sessionToken = "a-session-token";
        String body = "Because the wave bends around the edge of the gap.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        QuestionAttemptEntity attemptEntity = new QuestionAttemptEntity(questionId, sessionToken, body);
        setField(attemptEntity, "id", id);
        setField(attemptEntity, "createdAt", createdAt);

        QuestionAttempt questionAttempt = toQuestionAttempt(attemptEntity);

        assertThat(questionAttempt.id(), equalTo(id));
        assertThat(questionAttempt.questionId(), equalTo(questionId));
        // Carried through the service layer; the web mapper is what drops it.
        assertThat(questionAttempt.sessionToken(), equalTo(sessionToken));
        assertThat(questionAttempt.body(), equalTo(body));
        assertThat(questionAttempt.createdAt(), equalTo(createdAt));
    }
}
