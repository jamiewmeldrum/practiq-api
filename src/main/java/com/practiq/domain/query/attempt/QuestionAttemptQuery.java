package com.practiq.domain.query.attempt;

import com.practiq.domain.query.UserRestrictedQuery;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;


@Builder
@Getter
@EqualsAndHashCode
public class QuestionAttemptQuery implements UserRestrictedQuery {
    private long questionId;
    private String sessionToken;
}