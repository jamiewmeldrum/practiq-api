package com.practiq.dto.mapper;

import com.practiq.domain.Question;
import com.practiq.domain.projection.LinkedQuestion;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import com.practiq.dto.response.QuestionResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static com.practiq.dto.mapper.QuestionResponseMapper.toQuestionResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static utils.TestReflection.setField;

class QuestionResponseMapperTest {

    @Test
    void linkedQuestionMapsToQuestionResponse() {
        long id = 1L;
        QuestionDifficulty difficulty = QuestionDifficulty.MEDIUM;
        QuestionType type = QuestionType.SHORT_ANSWER;
        String body = "Diffraction";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        Question question = new Question(
                body,
                difficulty,
                type,
                QuestionSource.SEED,
                QuestionStatus.APPROVED,
                "AQA GCSE Physics"
        );
        setField(question, "id", id);
        setField(question, "createdAt", createdAt);

        long conceptId1 = 1L;
        long conceptId2 = 2L;
        LinkedQuestion linkedQuestion = new LinkedQuestion(
                question,
                Set.of(new QuestionConceptLink(id, conceptId1), new QuestionConceptLink(id, conceptId2)));

        QuestionResponse questionResponse = toQuestionResponse(linkedQuestion);

        assertThat(questionResponse.getId(), equalTo(id));
        assertThat(questionResponse.getBody(), equalTo(body));
        assertThat(questionResponse.getDifficulty().getValue(), equalTo(difficulty.value()));
        assertThat(questionResponse.getDifficulty().getCode(), equalTo(difficulty.name()));
        assertThat(questionResponse.getType(), equalTo(type));
        assertThat(questionResponse.getCreatedAt(), equalTo(createdAt));
        assertThat(questionResponse.getLinkedConceptIds(), equalTo(Set.of(conceptId1, conceptId2)));
    }

    // A bare question: an unrated difficulty and absent type both serialise as null (difficulty as a whole
    // object, never partially populated), and no concept links yields an empty set rather than null.
    @Test
    void bareLinkedQuestionMapsNullDifficultyAndTypeAndEmptyLinks() {
        long id = 2L;
        String body = "Explain what is meant by diffraction.";
        Instant createdAt = Instant.parse("2026-01-02T00:00:00Z");

        Question question = new Question(
                body,
                null,
                null,
                QuestionSource.SEED,
                QuestionStatus.APPROVED,
                null
        );
        setField(question, "id", id);
        setField(question, "createdAt", createdAt);

        LinkedQuestion linkedQuestion = new LinkedQuestion(question, Set.of());

        QuestionResponse questionResponse = toQuestionResponse(linkedQuestion);

        assertThat(questionResponse.getId(), equalTo(id));
        assertThat(questionResponse.getBody(), equalTo(body));
        assertThat(questionResponse.getDifficulty(), nullValue());
        assertThat(questionResponse.getType(), nullValue());
        assertThat(questionResponse.getCreatedAt(), equalTo(createdAt));
        assertThat(questionResponse.getLinkedConceptIds(), equalTo(Set.of()));
    }
}
