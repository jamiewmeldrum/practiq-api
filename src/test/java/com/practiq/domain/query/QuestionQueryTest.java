package com.practiq.domain.query;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionQueryTest {
    @Test
    void studentCatalogueForGetAllFilteringSetsMandatoryAndSpecifiedFields() {
        List<QuestionType> types = List.of(QuestionType.MCQ);
        List<QuestionDifficulty> difficulties = List.of(QuestionDifficulty.HARD);
        long conceptId = 42L;

        QuestionQuery query = QuestionQuery.studentCatalogue(types, difficulties, conceptId);

        assertEquals(QuestionStatus.APPROVED, query.getStatus());
        assertTrue(query.isRequiresConceptLink());
        assertThat(query.getTypes(), contains(QuestionType.MCQ));
        assertThat(query.getDifficulties(), contains(QuestionDifficulty.HARD));
        assertEquals(conceptId, query.getConceptId());
    }

    @Test
    void studentCatalogueForGetOneSetsMandatoryAndSpecifiedFields() {
        long questionId = 15L;

        QuestionQuery query = QuestionQuery.studentCatalogue(questionId);

        assertEquals(QuestionStatus.APPROVED, query.getStatus());
        assertTrue(query.isRequiresConceptLink());
        assertEquals(questionId, query.getQuestionId());
    }
}
