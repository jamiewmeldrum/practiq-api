package com.practiq.web.dto.mapper;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.service.question.dto.response.Question;
import com.practiq.web.dto.response.QuestionDifficultyResponse;
import com.practiq.web.dto.response.QuestionResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuestionResponseMapper {

    public static List<QuestionResponse> toQuestionResponses(List<Question> questions) {
        return questions.stream()
                .map(QuestionResponseMapper::toQuestionResponse)
                .toList();
    }

    public static QuestionResponse toQuestionResponse(Question question) {
        log.trace("Converting question to QuestionResponse: {}", question.id());

        QuestionDifficulty difficulty = question.difficulty();
        return new QuestionResponse(
                question.id(),
                question.body(),
                difficulty == null ? null : new QuestionDifficultyResponse(difficulty),
                question.type(),
                question.createdAt(),
                question.linkedConceptIds());
    }
}
