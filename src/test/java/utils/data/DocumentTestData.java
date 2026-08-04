package utils.data;

import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class DocumentTestData extends TestData {

    public DocumentTestData(TestDatabase testDatabase) {
        super(testDatabase);
    }

    @Override
    public void clear() {
        testDatabase.clear(DOCUMENT);
    }

    public DocumentRow document() {
        return new DocumentRow();
    }

    public DocumentRow document(String s3Key, String filename) {
        return new DocumentRow(s3Key, filename);
    }

    public List<DBRow> retrieveDocuments() {
        return testDatabase.selectAll(DOCUMENT);
    }
}
