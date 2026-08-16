package com.practiq.service.attempt.mapper;

import com.practiq.persistence.QuestionAttemptEntity;
import com.practiq.service.attempt.dto.response.QuestionAttempt;

public class QuestionAttemptMapper {

    public static QuestionAttempt toQuestionAttempt(QuestionAttemptEntity questionAttemptEntity) {
        return new QuestionAttempt(
                questionAttemptEntity.getId(),
                questionAttemptEntity.getQuestionId(),
                questionAttemptEntity.getSessionToken(),
                questionAttemptEntity.getBody(),
                questionAttemptEntity.getCreatedAt());
    }
}
