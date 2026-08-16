package com.practiq.service.question;

import static com.practiq.service.question.QuestionAccessorFactory.STUDENT;

import com.practiq.service.question.dto.request.QuestionSearchCriteria;
import com.practiq.service.question.dto.response.Question;
import com.practiq.service.question.mapper.QuestionMapper;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class QuestionService {

    private final QuestionAccessor questions;

    public QuestionService(@Named(STUDENT) QuestionAccessor questions) {
        this.questions = questions;
    }

    @Transactional(readOnly = true)
    public Optional<Question> getById(long id) {
        log.debug("Getting question for id {}", id);
        return questions.findById(id).map(QuestionMapper::toQuestion);
    }

    @Transactional(readOnly = true)
    public Page<Question> get(QuestionSearchCriteria criteria, Pageable pageable) {
        log.debug("Getting approved questions, page {}", pageable.getNumber());
        return questions.findPage(criteria, pageable).map(QuestionMapper::toQuestion);
    }
}
