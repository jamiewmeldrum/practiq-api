package integration.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static utils.TestReflection.setField;
import static utils.data.TestData.*;

import com.practiq.persistence.DocumentEntity;
import com.practiq.persistence.repository.DocumentRepository;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
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

        DocumentEntity document = documentRepository.findAll().getFirst();
        assertThat(document.getVersion(), equalTo(0));

        setField(document, "filename", "modified filename");
        documentRepository.update(document);

        DocumentEntity modifiedDocument = documentRepository.findAll().getFirst();
        assertThat(modifiedDocument.getVersion(), equalTo(1));
    }

    @Test
    void ensureStaleVersionUpdateIsRejected() {
        data.document("s3key", "filename").insert();

        DocumentEntity stale = documentRepository.findAll().getFirst();

        // A concurrent editor wins the race: the row moves to version 1.
        DocumentEntity current = documentRepository.findAll().getFirst();
        setField(current, "filename", "Updated first.");
        documentRepository.update(current);

        // Writing through the stale copy (still version 0) must fail, and the winner's write must survive.
        setField(stale, "filename", "Updated second, from stale state.");
        assertThrows(OptimisticLockException.class, () -> documentRepository.update(stale));

        DocumentEntity survivor = documentRepository.findAll().getFirst();
        assertThat(survivor.getFilename(), equalTo("Updated first."));
        assertThat(survivor.getVersion(), equalTo(1));
    }

    @Test
    void cannotSaveDocumentWithTooLongSourceSpec() {
        data.document("s3key", "filename").insert();

        // Check 255 chars saves
        DocumentEntity valid = documentRepository.findAll().getFirst();
        setField(valid, "sourceSpec", RandomStringUtils.insecure().nextAlphanumeric(DOCUMENT_SOURCE_SPEC_MAX_LENGTH));
        documentRepository.update(valid);
        assertThat(documentRepository.findAll().getFirst().getVersion(), equalTo(1));

        // Check 256 chars doesn't save
        DocumentEntity invalid = documentRepository.findAll().getFirst();
        setField(
                invalid,
                "sourceSpec",
                RandomStringUtils.insecure().nextAlphanumeric(DOCUMENT_SOURCE_SPEC_MAX_LENGTH + 1));
        ConstraintViolationException thrown =
                assertThrows(ConstraintViolationException.class, () -> documentRepository.update(invalid));

        Set<ConstraintViolation<?>> constraintViolations = thrown.getConstraintViolations();
        assertThat(constraintViolations.size(), is(1));
        String message = constraintViolations.stream().findFirst().get().getMessage();
        assertThat(message, equalTo("size must be between 0 and " + DOCUMENT_SOURCE_SPEC_MAX_LENGTH));
    }

    @Test
    void cannotSaveDocumentWithTooLongFilename() {
        data.document("s3key", "filename").insert();

        // Check 255 chars saves
        DocumentEntity valid = documentRepository.findAll().getFirst();
        setField(valid, "filename", RandomStringUtils.insecure().nextAlphanumeric(DOCUMENT_FILENAME_MAX_LENGTH));
        documentRepository.update(valid);
        assertThat(documentRepository.findAll().getFirst().getVersion(), equalTo(1));

        // Check 256 chars doesn't save
        DocumentEntity invalid = documentRepository.findAll().getFirst();
        setField(invalid, "filename", RandomStringUtils.insecure().nextAlphanumeric(DOCUMENT_FILENAME_MAX_LENGTH + 1));
        ConstraintViolationException thrown =
                assertThrows(ConstraintViolationException.class, () -> documentRepository.update(invalid));

        Set<ConstraintViolation<?>> constraintViolations = thrown.getConstraintViolations();
        assertThat(constraintViolations.size(), is(1));
        String message = constraintViolations.stream().findFirst().get().getMessage();
        assertThat(message, equalTo("size must be between 0 and " + DOCUMENT_FILENAME_MAX_LENGTH));
    }

    @Test
    void cannotSaveDocumentWithTooLongS3Key() {
        data.document("s3key", "filename").insert();

        // Check 255 chars saves
        DocumentEntity valid = documentRepository.findAll().getFirst();
        setField(valid, "s3Key", RandomStringUtils.insecure().nextAlphanumeric(DOCUMENT_S3_KEY_MAX_LENGTH));
        documentRepository.update(valid);
        assertThat(documentRepository.findAll().getFirst().getVersion(), equalTo(1));

        // Check 256 chars doesn't save
        DocumentEntity invalid = documentRepository.findAll().getFirst();
        setField(invalid, "s3Key", RandomStringUtils.insecure().nextAlphanumeric(DOCUMENT_S3_KEY_MAX_LENGTH + 1));
        ConstraintViolationException thrown =
                assertThrows(ConstraintViolationException.class, () -> documentRepository.update(invalid));

        Set<ConstraintViolation<?>> constraintViolations = thrown.getConstraintViolations();
        assertThat(constraintViolations.size(), is(1));
        String message = constraintViolations.stream().findFirst().get().getMessage();
        assertThat(message, equalTo("size must be between 0 and " + DOCUMENT_S3_KEY_MAX_LENGTH));
    }

    @Test
    void cannotSaveDocumentWithoutStatus() {
        DocumentEntity invalidDocument = DocumentEntity.newUpload("s3key", "filename", null);
        setField(invalidDocument, "status", null);

        ConstraintViolationException thrown =
                assertThrows(ConstraintViolationException.class, () -> documentRepository.save(invalidDocument));

        Set<ConstraintViolation<?>> constraintViolations = thrown.getConstraintViolations();
        assertThat(constraintViolations.size(), is(1));
        String message = constraintViolations.stream().findFirst().get().getMessage();
        assertThat(message, equalTo("must not be null"));
    }
}
