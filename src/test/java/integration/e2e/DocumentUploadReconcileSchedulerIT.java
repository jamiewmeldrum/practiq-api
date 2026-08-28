package integration.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.practiq.foundation.types.DocumentStatus;
import com.practiq.service.document.DocumentUploadReconcileScheduler;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import utils.DateTimeUtils;
import utils.IntegrationTest;
import utils.aws.S3TestUtils;
import utils.data.DBRow;
import utils.data.TestData;

// Three rather than the production hundred so a backlog can be built from four rows. The other tests
// here stay well under it, so an off-by-one in the limit fails the backlog test and only that one.
@IntegrationTest
@Property(name = "practiq.document-upload-reconcile.batch-size", value = "3")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentUploadReconcileSchedulerIT {

    private static final String TARGET_BUCKET = "documents";

    @Inject
    private TestData data;

    @Inject
    private S3TestUtils s3TestUtils;

    @Inject
    private DocumentUploadReconcileScheduler scheduler;

    @BeforeAll
    void createBucket() {
        s3TestUtils.ensureBucketExistsAndIsEmpty(TARGET_BUCKET);
    }

    @BeforeEach
    void setUp() {
        data.clear();
        s3TestUtils.emptyBucket(TARGET_BUCKET);
    }

    @Test
    void promotesADocumentWhoseUploadReachedStorage() {
        OffsetDateTime beyondTheUploadWindow = DateTimeUtils.now().minusMinutes(20);
        String uploadedKey = "uploaded.txt";

        data.document(uploadedKey, "uploaded.txt")
                .id(41L)
                .status(DocumentStatus.AWAITING_UPLOAD)
                .createdAt(beyondTheUploadWindow)
                .insert();
        data.document("abandoned.txt", "abandoned.txt")
                .id(42L)
                .status(DocumentStatus.AWAITING_UPLOAD)
                .createdAt(beyondTheUploadWindow)
                .insert();
        s3TestUtils.createObject(TARGET_BUCKET, uploadedKey, "the file that arrived");

        scheduler.runUploadReconcile();

        List<DBRow> documents = data.retrieveDocuments();
        assertThat(DBRow.collectColumn(documents, "id"), contains(41L));
        assertThat(documents.getFirst().get("status"), equalTo(DocumentStatus.UNAPPROVED.name()));
    }

    @Test
    void deletesADocumentWhoseUploadNeverReachedStorage() {
        OffsetDateTime beyondTheUploadWindow = DateTimeUtils.now().minusMinutes(20);
        String uploadedKey = "uploaded.txt";

        data.document(uploadedKey, "uploaded.txt")
                .id(51L)
                .status(DocumentStatus.AWAITING_UPLOAD)
                .createdAt(beyondTheUploadWindow)
                .insert();
        data.document("abandoned.txt", "abandoned.txt")
                .id(52L)
                .status(DocumentStatus.AWAITING_UPLOAD)
                .createdAt(beyondTheUploadWindow)
                .insert();
        s3TestUtils.createObject(TARGET_BUCKET, uploadedKey, "the file that arrived");

        scheduler.runUploadReconcile();

        List<DBRow> documents = data.retrieveDocuments();
        assertThat(DBRow.collectColumn(documents, "id"), contains(51L));
    }

    @Test
    void worksThroughABacklogOldestFirstAcrossSuccessiveRuns() {
        OffsetDateTime oldest = DateTimeUtils.now().minusMinutes(50);

        // Inserted newest-first: with the rows in the order the run should pick them, an unordered query
        // would return them correctly by physical scan order and this would pass having proven nothing.
        data.document("fourth.txt", "fourth.txt")
                .id(74L)
                .status(DocumentStatus.AWAITING_UPLOAD)
                .createdAt(oldest.plusMinutes(15))
                .insert();
        data.document("third.txt", "third.txt")
                .id(73L)
                .status(DocumentStatus.AWAITING_UPLOAD)
                .createdAt(oldest.plusMinutes(10))
                .insert();
        data.document("second.txt", "second.txt")
                .id(72L)
                .status(DocumentStatus.AWAITING_UPLOAD)
                .createdAt(oldest.plusMinutes(5))
                .insert();
        data.document("first.txt", "first.txt")
                .id(71L)
                .status(DocumentStatus.AWAITING_UPLOAD)
                .createdAt(oldest)
                .insert();

        scheduler.runUploadReconcile();

        // Only the newest is left: a run takes the batch size, and takes the oldest of what is waiting.
        assertThat(DBRow.collectColumn(data.retrieveDocuments(), "id"), contains(74L));

        scheduler.runUploadReconcile();

        assertThat(data.retrieveDocuments(), empty());
    }

    @Test
    void leavesADocumentStillInsideItsUploadWindow() {
        OffsetDateTime withinTheUploadWindow = DateTimeUtils.now().minusMinutes(5);

        data.document("still-uploading.txt", "still-uploading.txt")
                .id(61L)
                .status(DocumentStatus.AWAITING_UPLOAD)
                .createdAt(withinTheUploadWindow)
                .insert();

        scheduler.runUploadReconcile();

        List<DBRow> documents = data.retrieveDocuments();
        assertThat(DBRow.collectColumn(documents, "id"), contains(61L));
        assertThat(documents.getFirst().get("status"), equalTo(DocumentStatus.AWAITING_UPLOAD.name()));
    }
}
