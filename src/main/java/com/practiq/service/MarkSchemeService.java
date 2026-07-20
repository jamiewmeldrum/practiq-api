package com.practiq.service;

import com.practiq.domain.query.StudentQuestionQueryRunner;
import com.practiq.dto.mapper.MarkSchemeResponseMapper;
import com.practiq.dto.response.MarkSchemeResponse;
import com.practiq.repository.MarkSchemeRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Singleton
public class MarkSchemeService {

    private final StudentQuestionQueryRunner questionQueryRunner;
    private final MarkSchemeRepository markSchemeRepository;

    public MarkSchemeService(StudentQuestionQueryRunner questionQueryRunner, MarkSchemeRepository markSchemeRepository) {
        this.questionQueryRunner = questionQueryRunner;
        this.markSchemeRepository = markSchemeRepository;
    }

    @Transactional(readOnly = true)
    public Optional<MarkSchemeResponse> getForQuestionId(long questionId) {
        log.debug("Getting mark scheme for question id: {}", questionId);

        if (questionQueryRunner.doesQuestionExistForId(questionId)) {
            return markSchemeRepository.findByQuestionId(questionId).map(MarkSchemeResponseMapper::toMarkSchemeResponse);
        } else {
            return Optional.empty();
        }
    }
}
