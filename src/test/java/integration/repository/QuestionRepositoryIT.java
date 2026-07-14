package integration.repository;

import com.practiq.domain.Question;
import com.practiq.domain.query.QuestionQuery;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.repository.QuestionRepository;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import utils.IntegrationTest;
import utils.data.QuestionTestData;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.OptimisticLockException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static utils.TestReflection.setField;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@IntegrationTest
class QuestionRepositoryIT {

    private static final Sort STABLE_ORDER = Sort.of(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    @Inject
    private QuestionRepository questionRepository;

    @Inject
    private QuestionTestData data;

    @Inject
    private QuestionSpecificationFactory questionSpecificationFactory;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void ensureVersionIncrements() {
        data.question()
                .body("A question.")
                .source(QuestionSource.SEED)
                .insert();

        Question question = questionRepository.findAll().getFirst();
        assertThat(question.getVersion(), equalTo(0));

        setField(question, "body", "A modified question.");
        questionRepository.update(question);

        Question modifiedQuestion = questionRepository.findAll().getFirst();
        assertThat(modifiedQuestion.getVersion(), equalTo(1));
    }

    // The other half of @Version: incrementing is only useful if a stale write actually fails. A copy
    // fetched before someone else's update still carries the old version, so writing through it must be
    // rejected rather than silently clobbering the newer row (lost update).
    @Test
    void ensureStaleVersionUpdateIsRejected() {
        data.question()
                .body("A question.")
                .source(QuestionSource.SEED)
                .insert();

        Question stale = questionRepository.findAll().getFirst();

        // A concurrent editor wins the race: the row moves to version 1.
        Question current = questionRepository.findAll().getFirst();
        setField(current, "body", "Updated first.");
        questionRepository.update(current);

        // Writing through the stale copy (still version 0) must fail, and the winner's write must survive.
        setField(stale, "body", "Updated second, from stale state.");
        assertThrows(OptimisticLockException.class, () -> questionRepository.update(stale));

        Question survivor = questionRepository.findAll().getFirst();
        assertThat(survivor.getBody(), equalTo("Updated first."));
        assertThat(survivor.getVersion(), equalTo(1));
    }

    @Test
    void pagedQueryReturnsQuestionsInStableCreatedAtThenIdOrder() {
        OffsetDateTime earlier = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime later = OffsetDateTime.parse("2026-01-02T00:00:00Z");

        long conceptId = 10L;
        data.concept(conceptId).insert();

        // earliestByTime has the highest id but the earliest created_at; the other two share a created_at
        // so only the id tiebreak separates them. Expected order: created_at first (earliestByTime leads),
        // then id ascending within the equal-timestamp pair.
        long earliestByTime = 3L;
        long sameTimeLowId = 1L;
        long sameTimeHighId = 2L;
        data.question(earliestByTime).status(QuestionStatus.APPROVED).createdAt(earlier).insert();
        data.question(sameTimeLowId).status(QuestionStatus.APPROVED).createdAt(later).insert();
        data.question(sameTimeHighId).status(QuestionStatus.APPROVED).createdAt(later).insert();
        data.link(earliestByTime, conceptId).insert();
        data.link(sameTimeLowId, conceptId).insert();
        data.link(sameTimeHighId, conceptId).insert();

        QuestionQuery query = QuestionQuery.builder()
                .status(QuestionStatus.APPROVED)
                .conceptId(conceptId)
                .requiresConceptLink(true)
                .build();

        QuerySpecification<Question> spec = questionSpecificationFactory.forQuery(query);
        Pageable ordered = Pageable.from(0, 10, STABLE_ORDER);

        List<Question> results = questionRepository.findAll(spec, ordered).getContent();

        assertThat(ids(results), contains(earliestByTime, sameTimeLowId, sameTimeHighId));
    }

    @Test
    void findByIdAndStatusReturnsOptionalEmptyIfNoMatchOnId() {
        long id = 1L;
        QuestionStatus status = QuestionStatus.APPROVED;
        data.question(id).status(status).insert();

        long conceptId = 10L;
        data.concept(conceptId).insert();

        data.link(id, conceptId).insert();

        QuestionQuery query = QuestionQuery.studentCatalogue(2L);
        QuerySpecification<Question> spec = questionSpecificationFactory.forQuery(query);
        Optional<Question> question = questionRepository.findOne(spec);

        assertThat(question.isPresent(), is(false));
    }

    @Test
    void findByIdAndStatusReturnsOptionalEmptyIfNotApproved() {
        long id = 1L;
        QuestionStatus status = QuestionStatus.REJECTED;
        data.question(id).status(status).insert();

        long conceptId = 10L;
        data.concept(conceptId).insert();

        data.link(id, conceptId).insert();

        QuestionQuery query = QuestionQuery.studentCatalogue(id);
        QuerySpecification<Question> spec = questionSpecificationFactory.forQuery(query);
        Optional<Question> question = questionRepository.findOne(spec);
        assertThat(question.isPresent(), is(false));
    }

    @Test
    void findByIdAndStatusReturnsOptionalEmptyIfNoQuestionConceptLinks() {
        long id = 1L;
        QuestionStatus status = QuestionStatus.APPROVED;
        data.question(id).status(status).insert();

        QuestionQuery query = QuestionQuery.studentCatalogue(id);
        QuerySpecification<Question> spec = questionSpecificationFactory.forQuery(query);
        Optional<Question> question = questionRepository.findOne(spec);
        assertThat(question.isPresent(), is(false));
    }

    @Test
    void findByIdAndStatusReturnsOptionalQuestionIfMatched() {
        long id = 1L;
        QuestionStatus status = QuestionStatus.APPROVED;
        data.question(id).status(status).insert();

        long conceptId = 10L;
        data.concept(conceptId).insert();

        data.link(id, conceptId).insert();

        QuestionQuery query = QuestionQuery.studentCatalogue(id);
        QuerySpecification<Question> spec = questionSpecificationFactory.forQuery(query);
        Optional<Question> question = questionRepository.findOne(spec);

        assertThat(question.isPresent(), is(true));
        assertThat(question.get().getId(), is(id));
        assertThat(question.get().getStatus(), is(status));
    }

    private static List<Long> ids(List<Question> questions) {
        return questions.stream().map(Question::getId).collect(toList());
    }
}
