package com.practiq.service.question.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionType;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionSearchCriteriaTest {

    @Test
    void criteriaHoldTheValuesTheyWereBuiltWith() {
        List<QuestionType> types = List.of(QuestionType.MCQ);
        List<QuestionDifficulty> difficulties = List.of(QuestionDifficulty.HARD);

        QuestionSearchCriteria criteria = new QuestionSearchCriteria(types, difficulties, 42L);

        assertEquals(types, criteria.types());
        assertEquals(difficulties, criteria.difficulties());
        assertEquals(42L, criteria.conceptId());
    }

    @Test
    void everyFilterIsOptional() {
        QuestionSearchCriteria criteria = new QuestionSearchCriteria(null, null, null);

        assertNull(criteria.types());
        assertNull(criteria.difficulties());
        assertNull(criteria.conceptId());
    }

    @Test
    void criteriaCannotBeBuiltWithAConceptIdBelowOne() {
        assertEquals(
                "conceptId must be greater than 0",
                assertThrows(IllegalArgumentException.class, () -> new QuestionSearchCriteria(null, null, 0L))
                        .getMessage());
    }
}
