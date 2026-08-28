package integration.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

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

// Five rather than the production hundred so a backlog can be built from six rows. The other tests here
// stay under it, so an off-by-one in the limit fails the backlog test and only that one.
@IntegrationTest
@Property(name = "practiq.document-upload-reconcile.batch-size", value = "5")
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
    void promotesEveryDocumentWhoseUploadReachedStorage() {
        OffsetDateTime beyondTheUploadWindow = DateTimeUtils.now().minusMinutes(30);

        aDocumentAwaitingUpload(41L, "uploaded-one.txt", beyondTheUploadWindow).insert();
        aDocumentAwaitingUpload(42L, "uploaded-two.txt", beyondTheUploadWindow).insert();
        aDocumentAwaitingUpload(43L, "abandoned-one.txt", beyondTheUploadWindow).insert();
        aDocumentAwaitingUpload(44L, "abandoned-two.txt", beyondTheUploadWindow).insert();
        s3TestUtils.createObject(TARGET_BUCKET, "uploaded-one.txt", "the first file that arrived");
        s3TestUtils.createObject(TARGET_BUCKET, "uploaded-two.txt", "the second file that arrived");

        scheduler.runUploadReconcile();

        List<DBRow> documents = data.retrieveDocuments();
        assertThat(DBRow.collectColumn(documents, "id"), containsInAnyOrder(41L, 42L));
        assertThat(
                DBRow.collectColumn(documents, "status"),
                contains(DocumentStatus.UNAPPROVED.name(), DocumentStatus.UNAPPROVED.name()));
    }

    @Test
    void deletesEveryDocumentWhoseUploadNeverReachedStorage() {
        OffsetDateTime beyondTheUploadWindow = DateTimeUtils.now().minusMinutes(30);

        aDocumentAwaitingUpload(51L, "uploaded-one.txt", beyondTheUploadWindow).insert();
        aDocumentAwaitingUpload(52L, "uploaded-two.txt", beyondTheUploadWindow).insert();
        aDocumentAwaitingUpload(53L, "abandoned-one.txt", beyondTheUploadWindow).insert();
        aDocumentAwaitingUpload(54L, "abandoned-two.txt", beyondTheUploadWindow).insert();
        s3TestUtils.createObject(TARGET_BUCKET, "uploaded-one.txt", "the first file that arrived");
        s3TestUtils.createObject(TARGET_BUCKET, "uploaded-two.txt", "the second file that arrived");

        scheduler.runUploadReconcile();

        // The survivors are exactly the two uploaded: both abandoned rows are gone, and the run did not
        // simply delete everything it looked at.
        assertThat(DBRow.collectColumn(data.retrieveDocuments(), "id"), containsInAnyOrder(51L, 52L));
    }

    @Test
    void leavesEveryDocumentStillInsideItsUploadWindow() {
        OffsetDateTime withinTheUploadWindow = DateTimeUtils.now().minusMinutes(5);

        aDocumentAwaitingUpload(61L, "still-uploading-one.txt", withinTheUploadWindow)
                .insert();
        aDocumentAwaitingUpload(62L, "still-uploading-two.txt", withinTheUploadWindow)
                .insert();

        scheduler.runUploadReconcile();

        List<DBRow> documents = data.retrieveDocuments();
        assertThat(DBRow.collectColumn(documents, "id"), containsInAnyOrder(61L, 62L));
        assertThat(
                DBRow.collectColumn(documents, "status"),
                contains(DocumentStatus.AWAITING_UPLOAD.name(), DocumentStatus.AWAITING_UPLOAD.name()));
    }

    @Test
    void worksThroughABacklogOldestFirstAcrossSuccessiveRuns() {
        OffsetDateTime oldest = DateTimeUtils.now().minusMinutes(50);

        // Inserted newest-first: with the rows in the order the run should pick them, an unordered query
        // would return them correctly by physical scan order and this would pass having proven nothing.
        aDocumentAwaitingUpload(76L, "sixth.txt", oldest.plusMinutes(25)).insert();
        aDocumentAwaitingUpload(75L, "fifth.txt", oldest.plusMinutes(20)).insert();
        aDocumentAwaitingUpload(74L, "fourth.txt", oldest.plusMinutes(15)).insert();
        aDocumentAwaitingUpload(73L, "third.txt", oldest.plusMinutes(10)).insert();
        aDocumentAwaitingUpload(72L, "second.txt", oldest.plusMinutes(5)).insert();
        aDocumentAwaitingUpload(71L, "first.txt", oldest).insert();

        scheduler.runUploadReconcile();

        // Only the newest is left: a run takes the batch size, and takes the oldest of what is waiting.
        assertThat(DBRow.collectColumn(data.retrieveDocuments(), "id"), contains(76L));

        scheduler.runUploadReconcile();

        assertThat(data.retrieveDocuments(), empty());
    }

    private TestData.DocumentRow aDocumentAwaitingUpload(long id, String s3Key, OffsetDateTime createdAt) {
        return data.document(s3Key, s3Key)
                .id(id)
                .status(DocumentStatus.AWAITING_UPLOAD)
                .createdAt(createdAt);
    }
}
