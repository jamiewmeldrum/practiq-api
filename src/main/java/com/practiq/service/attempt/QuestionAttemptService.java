package com.practiq.service.attempt;

import static com.practiq.service.question.QuestionAccessorFactory.STUDENT;

import com.practiq.persistence.QuestionAttemptEntity;
import com.practiq.persistence.query.attempt.QuestionAttemptQuery;
import com.practiq.persistence.query.attempt.QuestionAttemptQueryRunner;
import com.practiq.persistence.repository.QuestionAttemptRepository;
import com.practiq.service.UserRef;
import com.practiq.service.attempt.dto.request.QuestionAttemptCommand;
import com.practiq.service.attempt.dto.response.QuestionAttempt;
import com.practiq.service.attempt.mapper.QuestionAttemptMapper;
import com.practiq.service.question.QuestionAccessor;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class QuestionAttemptService {

    private final QuestionAccessor questions;
    private final QuestionAttemptQueryRunner questionAttemptQueryRunner;
    private final QuestionAttemptRepository questionAttemptRepository;

    public QuestionAttemptService(
            @Named(STUDENT) QuestionAccessor questions,
            QuestionAttemptQueryRunner questionAttemptQueryRunner,
            QuestionAttemptRepository questionAttemptRepository) {
        this.questions = questions;
        this.questionAttemptQueryRunner = questionAttemptQueryRunner;
        this.questionAttemptRepository = questionAttemptRepository;
    }

    // An empty Optional means the question is not one this user may attempt; an empty list means they have
    // not attempted it yet. The caller needs to tell those apart to answer 404 rather than an empty array.
    @Transactional(readOnly = true)
    public Optional<List<QuestionAttempt>> getForQuestionId(UserRef userRef, long questionId) {
        log.debug("Getting question attempts for question id: {}", questionId);

        if (!questions.exists(questionId)) {
            return Optional.empty();
        }

        QuestionAttemptQuery query = new QuestionAttemptQuery(questionId, userRef.sessionToken());
        return Optional.of(questionAttemptQueryRunner.findAll(query).stream()
                .map(QuestionAttemptMapper::toQuestionAttempt)
                .toList());
    }

    @Transactional
    public Optional<QuestionAttempt> create(QuestionAttemptCommand command) {
        log.debug("Posting question attempt for question id: {}", command.questionId());

        if (!questions.exists(command.questionId())) {
            return Optional.empty();
        }

        QuestionAttemptEntity attempt = new QuestionAttemptEntity(
                command.questionId(), command.userRef().sessionToken(), command.body());

        return Optional.of(QuestionAttemptMapper.toQuestionAttempt(questionAttemptRepository.save(attempt)));
    }
}
