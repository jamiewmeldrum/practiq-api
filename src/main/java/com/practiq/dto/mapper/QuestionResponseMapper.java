package com.practiq.dto.mapper;

import com.practiq.domain.Question;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.dto.response.QuestionDifficultyResponse;
import com.practiq.dto.response.QuestionResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class QuestionResponseMapper {
    public static QuestionResponse toQuestionResponse(Question question, Set<Long> conceptIds) {
        log.trace("Converting question to QuestionResponse: {}", question.getId());

        QuestionDifficulty difficulty = question.getDifficulty();
        return new QuestionResponse(
                question.getId(),
                question.getBody(),
                difficulty == null ? null : new QuestionDifficultyResponse(difficulty),
                question.getType(),
                question.getCreatedAt(),
                conceptIds
        );
    }
}
