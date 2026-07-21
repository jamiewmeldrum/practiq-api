package com.practiq.domain.query.attempt;

import com.practiq.domain.query.UserRestrictedQuery;

public record QuestionAttemptQuery(long questionId, String sessionToken) implements UserRestrictedQuery {
}