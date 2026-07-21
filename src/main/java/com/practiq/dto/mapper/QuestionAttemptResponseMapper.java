package com.practiq.dto.mapper;

import com.practiq.domain.QuestionAttempt;
import com.practiq.dto.response.QuestionAttemptResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuestionAttemptResponseMapper {
    public static QuestionAttemptResponse toQuestionAttemptResponse(QuestionAttempt questionAttempt) {
        log.trace("Converting QuestionAttempt to QuestionAttemptResponse: {}", questionAttempt.getId());

        return new QuestionAttemptResponse(
                questionAttempt.getId(),
                questionAttempt.getQuestionId(),
                questionAttempt.getBody(),
                questionAttempt.getCreatedAt()
        );
    }
}
