package com.practiq.service.question.mapper;

import com.practiq.persistence.QuestionEntity;
import com.practiq.persistence.query.question.QuestionWithConceptIds;
import com.practiq.service.question.dto.response.Question;

public class QuestionMapper {

    public static Question toQuestion(QuestionWithConceptIds questionWithConceptIds) {
        QuestionEntity questionEntity = questionWithConceptIds.question();

        return new Question(
                questionEntity.getId(),
                questionEntity.getVersion(),
                questionEntity.getBody(),
                questionEntity.getDifficulty(),
                questionEntity.getType(),
                questionEntity.getStatus(),
                questionEntity.getCreatedAt(),
                questionWithConceptIds.conceptIds());
    }
}
