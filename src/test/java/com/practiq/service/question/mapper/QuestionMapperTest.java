package com.practiq.service.question.mapper;

import static com.practiq.service.question.mapper.QuestionMapper.toQuestion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static utils.TestReflection.setField;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionStatus;
import com.practiq.foundation.types.QuestionType;
import com.practiq.persistence.QuestionEntity;
import com.practiq.persistence.query.question.QuestionWithConceptIds;
import com.practiq.service.question.dto.response.Question;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuestionMapperTest {

    @Test
    void questionWithConceptIdsMapsToQuestion() {
        long id = 1L;
        int version = 4;
        String body = "Explain why a wave bends around an obstacle.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        QuestionEntity questionEntity =
                new QuestionEntity(body, QuestionDifficulty.HARD, QuestionType.EXTENDED, QuestionStatus.APPROVED);
        setField(questionEntity, "id", id);
        setField(questionEntity, "version", version);
        setField(questionEntity, "createdAt", createdAt);

        Question question = toQuestion(new QuestionWithConceptIds(questionEntity, Set.of(10L, 11L)));

        assertThat(question.id(), equalTo(id));
        // The service model carries the lock token and the status even though no web response exposes either.
        assertThat(question.version(), equalTo(version));
        assertThat(question.status(), equalTo(QuestionStatus.APPROVED));
        assertThat(question.body(), equalTo(body));
        assertThat(question.difficulty(), equalTo(QuestionDifficulty.HARD));
        assertThat(question.type(), equalTo(QuestionType.EXTENDED));
        assertThat(question.createdAt(), equalTo(createdAt));
        assertThat(question.linkedConceptIds(), containsInAnyOrder(10L, 11L));
    }
}
