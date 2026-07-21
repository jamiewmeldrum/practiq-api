package integration.repository;

import com.practiq.domain.QuestionAttempt;
import com.practiq.domain.query.attempt.QuestionAttemptQuery;
import com.practiq.domain.query.attempt.QuestionAttemptSpecificationFactory;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.repository.QuestionAttemptRepository;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.QuestionTestData;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

// Repository-method concerns for attempts. What forQuery filters on lives in
// QuestionAttemptSpecificationFactoryIT; this pins how the repository methods themselves behave — starting
// with the stable order findAll applies, and growing as the write path (POST attempts) adds persistence.
@IntegrationTest
public class QuestionAttemptRepositoryIT {

    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    @Inject
    private QuestionTestData data;

    @Inject
    private QuestionAttemptRepository questionAttemptRepository;

    @Inject
    private QuestionAttemptSpecificationFactory questionAttemptSpecificationFactory;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void findAllAppliesTheStableCreatedAtThenIdOrder() {
        long conceptId = 10L;
        data.concept(conceptId).insert();

        long questionId = 7L;
        data.question(questionId).status(QuestionStatus.APPROVED).insert();
        data.link(questionId, conceptId).insert();

        String sessionToken = "sessionToken";

        long attemptId1 = 1L;
        OffsetDateTime day1 = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        data.questionAttempt(questionId, sessionToken, "body").id(attemptId1).createdAt(day1).insert();

        long attemptId2 = 2L;
        data.questionAttempt(questionId, sessionToken, "body").id(attemptId2).createdAt(day1).insert();

        long attemptId3 = 3L;
        OffsetDateTime day2 = OffsetDateTime.parse("2026-01-02T00:00:00Z");
        data.questionAttempt(questionId, sessionToken, "body").id(attemptId3).createdAt(day2).insert();

        QuestionAttemptQuery query = new QuestionAttemptQuery(questionId, sessionToken);
        QuerySpecification<QuestionAttempt> spec = questionAttemptSpecificationFactory.forQuery(query);

        List<QuestionAttempt> attempts = questionAttemptRepository.findAll(spec, STABLE_ORDER);

        // Newest created_at first; the day1 tie between attempts 1 and 2 is broken by ascending id.
        List<Long> ids = attempts.stream().map(QuestionAttempt::getId).toList();
        assertThat(ids, contains(attemptId3, attemptId1, attemptId2));
    }
}
