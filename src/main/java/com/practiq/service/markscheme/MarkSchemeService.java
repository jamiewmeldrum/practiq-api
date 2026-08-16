package com.practiq.service.markscheme;

import static com.practiq.service.question.QuestionAccessorFactory.STUDENT;

import com.practiq.persistence.repository.MarkSchemeRepository;
import com.practiq.service.markscheme.dto.response.MarkScheme;
import com.practiq.service.markscheme.mapper.MarkSchemeMapper;
import com.practiq.service.question.QuestionAccessor;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class MarkSchemeService {

    private final QuestionAccessor questions;
    private final MarkSchemeRepository markSchemeRepository;

    public MarkSchemeService(@Named(STUDENT) QuestionAccessor questions, MarkSchemeRepository markSchemeRepository) {
        this.questions = questions;
        this.markSchemeRepository = markSchemeRepository;
    }

    @Transactional(readOnly = true)
    public Optional<MarkScheme> getForQuestionId(long questionId) {
        log.debug("Getting mark scheme for question id: {}", questionId);

        if (questions.exists(questionId)) {
            return markSchemeRepository.findByQuestionId(questionId).map(MarkSchemeMapper::toMarkScheme);
        } else {
            return Optional.empty();
        }
    }
}
