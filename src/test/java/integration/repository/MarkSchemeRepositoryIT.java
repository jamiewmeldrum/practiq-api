package integration.repository;

import com.practiq.domain.MarkScheme;
import com.practiq.repository.MarkSchemeRepository;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.QuestionTestData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static utils.TestReflection.setField;

@IntegrationTest
public class MarkSchemeRepositoryIT {

    @Inject
    private QuestionTestData data;

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

        MarkScheme markScheme = markSchemeRepository.findAll().getFirst();
        assertThat(markScheme.getVersion(), equalTo(0));

        setField(markScheme, "body", "A modified mark scheme.");
        markSchemeRepository.update(markScheme);

        MarkScheme modifiedMarkScheme = markSchemeRepository.findAll().getFirst();
        assertThat(modifiedMarkScheme.getVersion(), equalTo(1));
    }

    @Test
    void ensureStaleVersionUpdateIsRejected() {
        long questionId = 1L;
        data.question(questionId).insert();
        data.markScheme(questionId, "body").insert();

        MarkScheme stale = markSchemeRepository.findAll().getFirst();

        // A concurrent editor wins the race: the row moves to version 1.
        MarkScheme current = markSchemeRepository.findAll().getFirst();
        setField(current, "body", "Updated first.");
        markSchemeRepository.update(current);

        // Writing through the stale copy (still version 0) must fail, and the winner's write must survive.
        setField(stale, "body", "Updated second, from stale state.");
        assertThrows(OptimisticLockException.class, () -> markSchemeRepository.update(stale));

        MarkScheme survivor = markSchemeRepository.findAll().getFirst();
        assertThat(survivor.getBody(), equalTo("Updated first."));
        assertThat(survivor.getVersion(), equalTo(1));
    }
}
