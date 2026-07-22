package com.practiq.dto.mapper;

import com.practiq.domain.Question;
import com.practiq.domain.projection.LinkedQuestion;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.dto.response.QuestionDifficultyResponse;
import com.practiq.dto.response.QuestionResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class QuestionResponseMapper {

    public static List<QuestionResponse> toQuestionResponses(List<LinkedQuestion> linkedQuestions) {
        return linkedQuestions.stream()
                .map(QuestionResponseMapper::toQuestionResponse)
                .toList();
    }

    public static QuestionResponse toQuestionResponse(LinkedQuestion linkedQuestion) {
        Question question = linkedQuestion.question();
        log.trace("Converting question to QuestionResponse: {}", question.getId());

        QuestionDifficulty difficulty = question.getDifficulty();
        return new QuestionResponse(
                question.getId(),
                question.getBody(),
                difficulty == null ? null : new QuestionDifficultyResponse(difficulty),
                question.getType(),
                question.getCreatedAt(),
                linkedQuestion.conceptLinks().stream().map(QuestionConceptLink::conceptId).collect(toSet())
        );
    }
}
