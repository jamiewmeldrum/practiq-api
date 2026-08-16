package com.practiq.web.dto.mapper;

import static com.practiq.web.dto.mapper.QuestionResponseMapper.toQuestionResponse;
import static com.practiq.web.dto.mapper.QuestionResponseMapper.toQuestionResponses;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionStatus;
import com.practiq.foundation.types.QuestionType;
import com.practiq.service.question.dto.response.Question;
import com.practiq.web.dto.response.QuestionResponse;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuestionResponseMapperTest {

    @Test
    void questionMapsToQuestionResponse() {
        long id = 1L;
        String body = "Explain why a wave bends around an obstacle.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Set<Long> linkedConceptIds = Set.of(10L, 11L);

        // A non-zero version and a status the response has no field for: the web mapper selects a subset, so
        // neither may reach a client.
        Question question = new Question(
                id,
                7,
                body,
                QuestionDifficulty.HARD,
                QuestionType.EXTENDED,
                QuestionStatus.APPROVED,
                createdAt,
                linkedConceptIds);

        QuestionResponse questionResponse = toQuestionResponse(question);

        assertThat(questionResponse.id(), equalTo(id));
        assertThat(questionResponse.body(), equalTo(body));
        assertThat(questionResponse.difficulty().getValue(), equalTo(QuestionDifficulty.HARD.value()));
        assertThat(questionResponse.difficulty().getCode(), equalTo(QuestionDifficulty.HARD.name()));
        assertThat(questionResponse.type(), equalTo(QuestionType.EXTENDED));
        assertThat(questionResponse.createdAt(), equalTo(createdAt));
        assertThat(questionResponse.linkedConceptIds(), containsInAnyOrder(10L, 11L));
    }

    @Test
    void anUnratedQuestionMapsToANullDifficultyRatherThanAPartiallyPopulatedOne() {
        Question question = new Question(
                1L,
                0,
                "A question.",
                null,
                null,
                QuestionStatus.APPROVED,
                Instant.parse("2026-01-01T00:00:00Z"),
                Set.of());

        QuestionResponse questionResponse = toQuestionResponse(question);

        assertThat(questionResponse.difficulty(), nullValue());
        assertThat(questionResponse.type(), nullValue());
    }

    @Test
    void questionsMapToQuestionResponsesInOrder() {
        Question first = new Question(
                1L, 0, "First", null, null, QuestionStatus.APPROVED, Instant.parse("2026-01-01T00:00:00Z"), Set.of());
        Question second = new Question(
                2L, 0, "Second", null, null, QuestionStatus.APPROVED, Instant.parse("2026-01-02T00:00:00Z"), Set.of());

        List<QuestionResponse> questionResponses = toQuestionResponses(List.of(first, second));

        assertThat(questionResponses.stream().map(QuestionResponse::body).toList(), contains("First", "Second"));
    }
}
