package com.practiq.domain.query.attempt;

import com.practiq.domain.QuestionAttempt;
import com.practiq.domain.query.QueryRestriction;
import com.practiq.domain.query.SessionTokenRestriction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

// The filtering behaviour (question id + session token) is proven against the DB in the IT. This pins the one
// thing observable without a database: an attempt query always carries the session-token restriction, so a
// session can never be served another session's attempts. Same package so it can read the protected hook.
class QuestionAttemptSpecificationFactoryTest {

    @Test
    void declaresTheSessionTokenRestriction() {
        QuestionAttemptSpecificationFactory factory = new QuestionAttemptSpecificationFactory();

        List<QueryRestriction<QuestionAttempt, ? super QuestionAttemptQuery>> restrictions = factory.restrictions();

        assertThat(restrictions, hasSize(1));
        assertThat(restrictions.get(0), instanceOf(SessionTokenRestriction.class));
    }
}
