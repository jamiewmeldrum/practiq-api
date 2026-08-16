package com.practiq.persistence.query.attempt;

import com.practiq.persistence.query.UserRestrictedQuery;

public record QuestionAttemptQuery(long questionId, String sessionToken) implements UserRestrictedQuery {}
