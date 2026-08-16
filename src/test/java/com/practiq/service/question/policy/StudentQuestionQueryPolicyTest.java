package com.practiq.service.question.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionStatus;
import com.practiq.foundation.types.QuestionType;
import com.practiq.persistence.query.question.QuestionQuery;
import java.util.List;
import org.junit.jupiter.api.Test;

class StudentQuestionQueryPolicyTest {
    @Test
    void studentCatalogueForGetAllFilteringSetsMandatoryAndSpecifiedFields() {
        List<QuestionType> types = List.of(QuestionType.MCQ);
        List<QuestionDifficulty> difficulties = List.of(QuestionDifficulty.HARD);
        long conceptId = 42L;

        StudentQuestionQueryPolicy policy = new StudentQuestionQueryPolicy();
        QuestionQuery query = policy.catalogue(types, difficulties, conceptId);

        assertEquals(QuestionStatus.APPROVED, query.getStatus());
        assertTrue(query.isRequiresConceptLink());
        assertThat(query.getTypes(), contains(QuestionType.MCQ));
        assertThat(query.getDifficulties(), contains(QuestionDifficulty.HARD));
        assertEquals(conceptId, query.getConceptId());
    }

    @Test
    void studentCatalogueForGetOneSetsMandatoryAndSpecifiedFields() {
        long questionId = 15L;

        StudentQuestionQueryPolicy policy = new StudentQuestionQueryPolicy();
        QuestionQuery query = policy.forId(questionId);

        assertEquals(QuestionStatus.APPROVED, query.getStatus());
        assertTrue(query.isRequiresConceptLink());
        assertEquals(questionId, query.getQuestionId());
    }
}
