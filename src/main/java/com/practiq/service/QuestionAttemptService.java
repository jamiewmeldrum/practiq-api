package com.practiq.service;

import com.practiq.domain.query.StudentQuestionQueryRunner;
import com.practiq.dto.mapper.MarkSchemeResponseMapper;
import com.practiq.dto.mapper.QuestionAttemptResponseMapper;
import com.practiq.dto.response.QuestionAttemptResponse;
import com.practiq.repository.QuestionAttemptRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

//TODO - testing to shadow MarkSchemeServiceTest
@Slf4j
@Singleton
public class QuestionAttemptService {

    private final StudentQuestionQueryRunner questionQueryRunner;
    private final QuestionAttemptRepository questionAttemptRepository;

    public QuestionAttemptService(StudentQuestionQueryRunner questionQueryRunner, QuestionAttemptRepository questionAttemptRepository) {
        this.questionQueryRunner = questionQueryRunner;
        this.questionAttemptRepository = questionAttemptRepository;
    }

    @Transactional(readOnly = true)
    public Optional<QuestionAttemptResponse> getForQuestionId(long questionId) {
        log.debug("Getting question attempt for question id: {}", questionId);

        if (questionQueryRunner.doesQuestionExistForId(questionId)) {
            return questionAttemptRepository.findByQuestionId(questionId).map(QuestionAttemptResponseMapper::toQuestionAttemptResponse);
        } else {
            return Optional.empty();
        }
    }
}
