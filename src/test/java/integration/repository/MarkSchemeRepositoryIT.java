package integration.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static utils.TestReflection.setField;
import static utils.data.TestData.MARK_SCHEME_BODY_MAX_LENGTH;

import com.practiq.persistence.MarkSchemeEntity;
import com.practiq.persistence.repository.MarkSchemeRepository;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.TestData;

@IntegrationTest
class MarkSchemeRepositoryIT {

    @Inject
    private TestData data;

    @Inject
    private MarkSchemeRepository markSchemeRepository;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void ensureVersionIncrements() {
        long questionId = 1L;
        data.question(questionId).insert();
        data.markScheme(questionId, "body").insert();

        MarkSchemeEntity markScheme = markSchemeRepository.findAll().getFirst();
        assertThat(markScheme.getVersion(), equalTo(0));

        setField(markScheme, "body", "A modified mark scheme.");
        markSchemeRepository.update(markScheme);

        MarkSchemeEntity modifiedMarkScheme = markSchemeRepository.findAll().getFirst();
        assertThat(modifiedMarkScheme.getVersion(), equalTo(1));
    }

    @Test
    void ensureStaleVersionUpdateIsRejected() {
        long questionId = 1L;
        data.question(questionId).insert();
        data.markScheme(questionId, "body").insert();

        MarkSchemeEntity stale = markSchemeRepository.findAll().getFirst();

        // A concurrent editor wins the race: the row moves to version 1.
        MarkSchemeEntity current = markSchemeRepository.findAll().getFirst();
        setField(current, "body", "Updated first.");
        markSchemeRepository.update(current);

        // Writing through the stale copy (still version 0) must fail, and the winner's write must survive.
        setField(stale, "body", "Updated second, from stale state.");
        assertThrows(OptimisticLockException.class, () -> markSchemeRepository.update(stale));

        MarkSchemeEntity survivor = markSchemeRepository.findAll().getFirst();
        assertThat(survivor.getBody(), equalTo("Updated first."));
        assertThat(survivor.getVersion(), equalTo(1));
    }

    // Both cases carry two mark schemes. Without the second, a finder that returned any row would pass the
    // match case, and the no-match case would pass on an empty table.
    @Test
    void findByQuestionIdReturnsEmptyIfNoMatch() {
        data.question(7L).insert();
        data.markScheme(7L, "Mark scheme for seven.").insert();
        data.question(8L).insert();
        data.markScheme(8L, "Mark scheme for eight.").insert();

        // 9 is neither of the two rows present.
        assertThat(markSchemeRepository.findByQuestionId(9L).isPresent(), is(false));
    }

    @Test
    void findByQuestionIdReturnsMarkSchemeIfMatch() {
        long questionId = 7L;
        String body = "Mark scheme for seven.";

        data.question(questionId).insert();
        data.markScheme(questionId, body).insert();
        data.question(8L).insert();
        data.markScheme(8L, "Mark scheme for eight.").insert();

        Optional<MarkSchemeEntity> markScheme = markSchemeRepository.findByQuestionId(questionId);
        assertThat(markScheme.isPresent(), is(true));

        assertThat(markScheme.get().getQuestionId(), equalTo(questionId));
        assertThat(markScheme.get().getBody(), equalTo(body));
    }

    @Test
    void cannotSaveMarkSchemeWithTooLongBody() {
        long questionId = 7L;
        data.question(questionId).insert();

        String validBody = RandomStringUtils.insecure().nextAlphanumeric(MARK_SCHEME_BODY_MAX_LENGTH);
        MarkSchemeEntity validMarkScheme = markSchemeRepository.save(new MarkSchemeEntity(questionId, validBody));
        assertThat(validMarkScheme.getBody(), equalTo(validBody));

        long otherQuestionId = 8L;
        data.question(otherQuestionId).insert();

        String body = RandomStringUtils.insecure().nextAlphanumeric(MARK_SCHEME_BODY_MAX_LENGTH + 1);
        MarkSchemeEntity invalidMarkScheme = new MarkSchemeEntity(otherQuestionId, body);
        ConstraintViolationException thrown =
                assertThrows(ConstraintViolationException.class, () -> markSchemeRepository.save(invalidMarkScheme));

        Set<ConstraintViolation<?>> constraintViolations = thrown.getConstraintViolations();
        assertThat(constraintViolations.size(), is(1));
        String message = constraintViolations.stream().findFirst().get().getMessage();
        assertThat(message, equalTo("size must be between 0 and " + MARK_SCHEME_BODY_MAX_LENGTH));
    }
}
