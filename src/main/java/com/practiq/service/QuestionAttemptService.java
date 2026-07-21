package com.practiq.service;

import com.practiq.domain.query.attempt.QuestionAttemptQueryRunner;
import com.practiq.domain.query.question.StudentQuestionQueryRunner;
import com.practiq.dto.filter.UserRequestFilter;
import com.practiq.dto.mapper.QuestionAttemptResponseMapper;
import com.practiq.dto.response.QuestionAttemptResponse;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
public class QuestionAttemptService {

    private final StudentQuestionQueryRunner questionQueryRunner;
    private final QuestionAttemptQueryRunner questionAttemptQueryRunner;

    public QuestionAttemptService(
            StudentQuestionQueryRunner questionQueryRunner,
            QuestionAttemptQueryRunner questionAttemptQueryRunner) {
        this.questionQueryRunner = questionQueryRunner;
        this.questionAttemptQueryRunner = questionAttemptQueryRunner;
    }

    @Transactional(readOnly = true)
    public Optional<List<QuestionAttemptResponse>> getForQuestionId(
            UserRequestFilter userRequestFilter,
            long questionId
    ) {
        log.debug("Getting question attempt for question id: {}", questionId);

        if (questionQueryRunner.doesQuestionExistForId(questionId)) {
            return Optional.of(
                    questionAttemptQueryRunner.getQuestionAttempts(userRequestFilter, questionId)
                    .stream()
                    .map(QuestionAttemptResponseMapper::toQuestionAttemptResponse)
                    .toList()
            );
        } else {
            return Optional.empty();
        }
    }
}
