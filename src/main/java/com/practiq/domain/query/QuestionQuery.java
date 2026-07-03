package com.practiq.domain.query;

import com.practiq.domain.types.QuestionStatus;

public record QuestionQuery(QuestionStatus status) {

    // Status is passed in by the caller, never taken from a request — the service hard-codes
    // APPROVED so an incoming request can never widen what students see. Other filters will
    // come from the request as they're added.
    public static QuestionQuery from(QuestionStatus status) {
        return new QuestionQuery(status);
    }
}
