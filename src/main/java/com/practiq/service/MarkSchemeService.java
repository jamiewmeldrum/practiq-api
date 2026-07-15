package com.practiq.service;

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

    private final QuestionQueryManager questionQueryManager;
    private final MarkSchemeRepository markSchemeRepository;

    public MarkSchemeService(QuestionQueryManager questionQueryManager, MarkSchemeRepository markSchemeRepository) {
        this.questionQueryManager = questionQueryManager;
        this.markSchemeRepository = markSchemeRepository;
    }

    @Transactional(readOnly = true)
    public Optional<MarkSchemeResponse> getForQuestionId(long questionId) {
        log.debug("Getting mark scheme for question id: {}", questionId);

        return questionQueryManager.findQuestionByIdForStudent(questionId)
                .flatMap(question -> markSchemeRepository.findByQuestionId(question.getId()))
                .map(MarkSchemeResponseMapper::toMarkSchemeResponse);
    }
}
