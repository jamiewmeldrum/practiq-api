package com.practiq.web.dto.mapper;

import com.practiq.service.attempt.dto.response.QuestionAttempt;
import com.practiq.web.dto.response.QuestionAttemptResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuestionAttemptResponseMapper {

    public static List<QuestionAttemptResponse> toQuestionAttemptResponses(List<QuestionAttempt> questionAttempts) {
        return questionAttempts.stream()
                .map(QuestionAttemptResponseMapper::toQuestionAttemptResponse)
                .toList();
    }

    public static QuestionAttemptResponse toQuestionAttemptResponse(QuestionAttempt questionAttempt) {
        log.trace("Converting QuestionAttempt to QuestionAttemptResponse: {}", questionAttempt.id());

        // sessionToken is deliberately dropped: the caller supplied it, and echoing it back would put a
        // credential-shaped value into response bodies, logs and caches for no benefit.
        return new QuestionAttemptResponse(
                questionAttempt.id(),
                questionAttempt.questionId(),
                questionAttempt.body(),
                questionAttempt.createdAt());
    }
}
