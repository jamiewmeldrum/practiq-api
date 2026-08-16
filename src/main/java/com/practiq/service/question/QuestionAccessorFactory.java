package com.practiq.service.question;

import com.practiq.persistence.query.question.QuestionQueryRunner;
import com.practiq.service.question.policy.StudentQuestionQueryPolicy;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

// Produces one accessor per audience, each already carrying its policy. Injection sites name the audience
// they want, so the decision is made once in a field declaration rather than on every call. An admin
// accessor joins this class when the review path lands.
@Factory
public class QuestionAccessorFactory {

    public static final String STUDENT = "student";

    @Singleton
    @Named(STUDENT)
    public QuestionAccessor studentQuestionAccessor(
            QuestionQueryRunner questionQueryRunner, StudentQuestionQueryPolicy policy) {
        return new QuestionAccessor(questionQueryRunner, policy);
    }
}
