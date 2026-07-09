package integration.repository;

import com.practiq.domain.Question;
import com.practiq.domain.types.QuestionSource;
import com.practiq.repository.QuestionRepository;
import utils.IntegrationTest;
import utils.data.QuestionTestData;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.OptimisticLockException;

import static utils.TestReflection.setField;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@IntegrationTest
class QuestionRepositoryIT {

    @Inject
    private QuestionRepository questionRepository;

    @Inject
    private QuestionTestData data;

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
}
