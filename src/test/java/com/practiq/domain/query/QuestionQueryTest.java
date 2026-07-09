package com.practiq.domain.query;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionQueryTest {

    @Test
    void nullTypesNormaliseToEmptyList() {
        QuestionQuery query = new QuestionQuery(null, null, null, null, true);

        // The invariant the specification factory relies on: types() is never null, so it can call
        // isEmpty() without a guard.
        assertThat(query.types(), empty());
    }

    @Test
    void providedTypesAreCarriedThroughUnchanged() {
        List<QuestionType> types = List.of(QuestionType.SHORT_ANSWER, QuestionType.MCQ);

        QuestionQuery query = new QuestionQuery(types, null, null, null, true);

        assertThat(query.types(), contains(QuestionType.SHORT_ANSWER, QuestionType.MCQ));
    }

    @Test
    void nullDifficultiesNormaliseToEmptyList() {
        QuestionQuery query = new QuestionQuery(null, null, null, null, true);

        // The invariant the specification factory relies on: difficulties() is never null, so it can call
        // isEmpty() without a guard.
        assertThat(query.difficulties(), empty());
    }

    @Test
    void providedDifficultiesAreCarriedThroughUnchanged() {
        List<QuestionDifficulty> difficulties = List.of(QuestionDifficulty.EASY, QuestionDifficulty.HARD);

        QuestionQuery query = new QuestionQuery(null, difficulties, null, null, true);

        assertThat(query.difficulties(), contains(QuestionDifficulty.EASY, QuestionDifficulty.HARD));
    }

    @Test
    void statusRequirementAreCarriedThrough() {
        QuestionQuery query = new QuestionQuery(null, null, QuestionStatus.PENDING, null, false);
        assertEquals(QuestionStatus.PENDING, query.status());

        query = new QuestionQuery(null, null, QuestionStatus.APPROVED, null, false);
        assertEquals(QuestionStatus.APPROVED, query.status());
    }

    @Test
    void linkRequirementAreCarriedThrough() {
        QuestionQuery query = new QuestionQuery(null, null, null, null, true);
        assertTrue(query.requiresConceptLink());

        query = new QuestionQuery(null, null, null, null, false);
        assertFalse(query.requiresConceptLink());
    }

    // The student-catalogue constructor IS the serving policy: APPROVED and concept-linked are baked in,
    // so no request-derived path can construct a wider student query. Filters pass through unchanged.
    @Test
    void studentCatalogueBakesInApprovedStatusAndTheConceptLinkRequirement() {
        List<QuestionType> types = List.of(QuestionType.MCQ);
        List<QuestionDifficulty> difficulties = List.of(QuestionDifficulty.HARD);

        QuestionQuery query = QuestionQuery.studentCatalogue(types, difficulties, 42L);

        assertEquals(QuestionStatus.APPROVED, query.status());
        assertTrue(query.requiresConceptLink());
        assertThat(query.types(), contains(QuestionType.MCQ));
        assertThat(query.difficulties(), contains(QuestionDifficulty.HARD));
        assertEquals(42L, query.conceptId());
    }
}
