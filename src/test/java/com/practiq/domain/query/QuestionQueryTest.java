package com.practiq.domain.query;

import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionQueryTest {

    @Test
    void nullTypesNormaliseToEmptyList() {
        QuestionQuery query = new QuestionQuery(null, QuestionStatus.APPROVED);

        // The invariant the specification factory relies on: types() is never null, so it can call
        // isEmpty() without a guard.
        assertThat(query.types(), empty());
    }

    @Test
    void providedTypesAreCarriedThroughUnchanged() {
        List<QuestionType> types = List.of(QuestionType.SHORT_ANSWER, QuestionType.MCQ);

        QuestionQuery query = new QuestionQuery(types, QuestionStatus.APPROVED);

        assertThat(query.types(), contains(QuestionType.SHORT_ANSWER, QuestionType.MCQ));
    }

    @Test
    void statusIsCarriedThrough() {
        QuestionQuery query = new QuestionQuery(List.of(), QuestionStatus.PENDING);

        assertEquals(QuestionStatus.PENDING, query.status());
    }
}
