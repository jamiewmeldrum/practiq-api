package com.practiq.service.attempt.dto.request;

import com.practiq.foundation.util.StringUtil;
import com.practiq.service.UserRef;

// A request to record an attempt that has already been checked for shape at the wire. Reaching here with a
// bad value means a caller has a bug, so these fail loudly rather than becoming an EntityValidationException
// the client would see as a 422.
public record QuestionAttemptCommand(long questionId, UserRef userRef, String body) {

    public QuestionAttemptCommand {
        if (questionId < 1) {
            throw new IllegalArgumentException("questionId must be greater than or equal to 1");
        }

        // Only null needs checking: a UserRef that exists is blank-free by construction.
        if (userRef == null) {
            throw new IllegalArgumentException("userRef must not be null");
        }

        if (StringUtil.isBlank(body)) {
            throw new IllegalArgumentException("body must not be blank");
        }
    }
}
