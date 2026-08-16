package com.practiq.service;

import com.practiq.foundation.util.StringUtil;
import com.practiq.persistence.QuestionAttemptEntity;

// The user an operation is about — not necessarily the caller. Both guards are caller bugs rather than bad
// input: the wire rejects a blank or oversized session-token header with a 422 before anything constructs
// one of these, so reaching here with either means the value came from somewhere that never checked.
public record UserRef(String sessionToken) {

    public UserRef {
        if (StringUtil.isBlank(sessionToken)) {
            throw new IllegalArgumentException("sessionToken must not be blank");
        }

        if (sessionToken.length() > QuestionAttemptEntity.SESSION_TOKEN_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "sessionToken cannot exceed max length " + QuestionAttemptEntity.SESSION_TOKEN_MAX_LENGTH);
        }
    }
}
