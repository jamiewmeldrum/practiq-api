package integration.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static utils.TestReflection.setField;

import com.practiq.domain.Document;
import com.practiq.repository.DocumentRepository;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.IntegrationTest;
import utils.data.TestData;

@IntegrationTest
class DocumentRepositoryIT {

    @Inject
    private TestData data;

    @Inject
    private DocumentRepository documentRepository;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void ensureVersionIncrements() {
        data.document("s3key", "filename").insert();

        Document document = documentRepository.findAll().getFirst();
        assertThat(document.getVersion(), equalTo(0));

        setField(document, "filename", "modified filename");
        documentRepository.update(document);

        Document modifiedDocument = documentRepository.findAll().getFirst();
        assertThat(modifiedDocument.getVersion(), equalTo(1));
    }

    @Test
    void ensureStaleVersionUpdateIsRejected() {
        data.document("s3key", "filename").insert();

        Document stale = documentRepository.findAll().getFirst();

        // A concurrent editor wins the race: the row moves to version 1.
        Document current = documentRepository.findAll().getFirst();
        setField(current, "filename", "Updated first.");
        documentRepository.update(current);

        // Writing through the stale copy (still version 0) must fail, and the winner's write must survive.
        setField(stale, "filename", "Updated second, from stale state.");
        assertThrows(OptimisticLockException.class, () -> documentRepository.update(stale));

        Document survivor = documentRepository.findAll().getFirst();
        assertThat(survivor.getFilename(), equalTo("Updated first."));
        assertThat(survivor.getVersion(), equalTo(1));
    }
}
