package utils.data;

import com.practiq.domain.types.*;
import jakarta.inject.Singleton;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class TestData {

    public static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    private static final String CONCEPT_TABLE = "concept";
    private static final String QUESTION_TABLE = "question";
    private static final String QUESTION_CONCEPT_TABLE = "question_concept";
    private static final String MARK_SCHEME_TABLE = "mark_scheme";
    private static final String QUESTION_ATTEMPT = "question_attempt";
    private static final String DOCUMENT = "document";
    private static final String QUESTION_ORIGIN = "question_origin";

    private static final String ISO_8601_UTC = "\\d{4}-\\d{2}-\\d{2}T.*Z";

    protected final TestDatabase testDatabase;

    protected TestData(TestDatabase testDatabase) {
        this.testDatabase = testDatabase;
    }

    public void clear() {
        testDatabase.clear(QUESTION_ATTEMPT);
        testDatabase.clear(MARK_SCHEME_TABLE);
        testDatabase.clear(QUESTION_CONCEPT_TABLE);
        testDatabase.clear(QUESTION_ORIGIN);
        testDatabase.clear(DOCUMENT);
        testDatabase.clear(QUESTION_TABLE);
        testDatabase.clear(CONCEPT_TABLE);
    }

    public String getInstantPattern() {
        return ISO_8601_UTC;
    }

    public QuestionRow question() {
        return new QuestionRow();
    }

    public QuestionRow question(long id) {
        return new QuestionRow(id);
    }

    public QuestionRow question(String body) {
        return new QuestionRow(body);
    }

    public List<DBRow> retrieveQuestions() {
        return testDatabase.selectAll(QUESTION_TABLE);
    }

    public void deleteQuestion(long id) {
        testDatabase.delete(QUESTION_TABLE, id);
    }

    public ConceptRow concept() {
        return new ConceptRow();
    }

    public ConceptRow concept(long id) {
        return new ConceptRow(id);
    }

    public List<DBRow> retrieveConcepts() {
        return testDatabase.selectAll(CONCEPT_TABLE);
    }

    public void updateConcept(long id, String column, Object value) {
        testDatabase.update(CONCEPT_TABLE, id, column, value);
    }

    public void deleteConcept(long id) {
        testDatabase.delete(CONCEPT_TABLE, id);
    }

    public QuestionConceptRow link(long questionId, long conceptId) {
        return new QuestionConceptRow(questionId, conceptId);
    }

    public List<DBRow> retrieveLinks() {
        return testDatabase.selectAll(QUESTION_CONCEPT_TABLE);
    }

    public MarkSchemeRow markScheme() {
        return new MarkSchemeRow();
    }

    public MarkSchemeRow markScheme(long questionId, String body) {
        return new MarkSchemeRow(questionId, body);
    }

    public List<DBRow> retrieveMarkSchemes() {
        return testDatabase.selectAll(MARK_SCHEME_TABLE);
    }

    public QuestionAttemptRow questionAttempt() {
        return new QuestionAttemptRow();
    }

    public QuestionAttemptRow questionAttempt(long questionId, String sessionToken, String body) {
        return new QuestionAttemptRow(questionId, sessionToken, body);
    }

    public List<DBRow> retrieveQuestionAttempts() {
        return testDatabase.selectAll(QUESTION_ATTEMPT);
    }

    public void updateQuestionAttempt(long id, String column, Object value) {
        testDatabase.update(QUESTION_ATTEMPT, id, column, value);
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

    public QuestionOriginRow questionOrigin() {
        return new QuestionOriginRow();
    }

    public QuestionOriginRow questionOrigin(long questionId, QuestionAuthorship authorship) {
        return new QuestionOriginRow(questionId, authorship);
    }

    public List<DBRow> retrieveQuestionOrigins() {
        return testDatabase.selectAll(QUESTION_ORIGIN);
    }

    public final class QuestionRow {
        private final Map<String, Object> columns = new HashMap<>();

        QuestionRow() {}

        QuestionRow(long id) {
            columns.put("id", id);
            columns.put("body", "Question " + id);
        }

        QuestionRow(String body) {
            columns.put("body", body);
        }

        public QuestionRow id(long id) {
            columns.put("id", id);
            return this;
        }

        public QuestionRow body(String body) {
            columns.put("body", body);
            return this;
        }

        public QuestionRow difficulty(QuestionDifficulty difficulty) {
            columns.put("difficulty", difficulty.value());
            return this;
        }

        public QuestionRow difficulty(int difficulty) {
            columns.put("difficulty", difficulty);
            return this;
        }

        public QuestionRow type(QuestionType type) {
            columns.put("type", type.name());
            return this;
        }

        public QuestionRow status(QuestionStatus status) {
            columns.put("status", status.name());
            return this;
        }

        // Overrides the DB default (now()) so ordering/paging tests can pin created_at deterministically,
        // including equal timestamps that force the (created_at, id) tiebreak to decide the order.
        public QuestionRow createdAt(OffsetDateTime createdAt) {
            columns.put("created_at", createdAt);
            return this;
        }

        public void insert() {
            testDatabase.insert(QUESTION_TABLE, columns);
        }
    }

    public final class ConceptRow {
        private final Map<String, Object> columns = new HashMap<>();

        ConceptRow() {}

        ConceptRow(long id) {
            columns.put("id", id);
            columns.put("name", "name " + id);
            columns.put("description", "description" + id);
        }

        public ConceptRow id(long id) {
            columns.put("id", id);
            return this;
        }

        public ConceptRow name(String name) {
            columns.put("name", name);
            return this;
        }

        public ConceptRow description(String description) {
            columns.put("description", description);
            return this;
        }

        public ConceptRow createdAt(OffsetDateTime createdAt) {
            columns.put("created_at", createdAt);
            return this;
        }

        public void insert() {
            testDatabase.insert(CONCEPT_TABLE, columns);
        }
    }

    public final class QuestionConceptRow {
        private final Map<String, Object> columns = new HashMap<>();

        QuestionConceptRow(long questionId, long conceptId) {
            columns.put("question_id", questionId);
            columns.put("concept_id", conceptId);
        }

        public void insert() {
            testDatabase.insert(QUESTION_CONCEPT_TABLE, columns);
        }
    }

    public final class MarkSchemeRow {
        private final Map<String, Object> columns = new HashMap<>();

        MarkSchemeRow() {}

        MarkSchemeRow(long questionId, String body) {
            columns.put("question_id", questionId);
            columns.put("body", body);
        }

        public MarkSchemeRow id(long id) {
            columns.put("id", id);
            return this;
        }

        public MarkSchemeRow questionId(long questionId) {
            columns.put("question_id", questionId);
            return this;
        }

        public MarkSchemeRow body(String body) {
            columns.put("body", body);
            return this;
        }

        public MarkSchemeRow createdAt(OffsetDateTime createdAt) {
            columns.put("created_at", createdAt);
            return this;
        }

        public void insert() {
            testDatabase.insert(MARK_SCHEME_TABLE, columns);
        }
    }

    public final class QuestionAttemptRow {
        private final Map<String, Object> columns = new HashMap<>();

        QuestionAttemptRow() {}

        QuestionAttemptRow(long questionId, String sessionToken, String body) {
            columns.put("question_id", questionId);
            columns.put("session_token", sessionToken);
            columns.put("body", body);
        }

        public QuestionAttemptRow id(long id) {
            columns.put("id", id);
            return this;
        }

        public QuestionAttemptRow questionId(long questionId) {
            columns.put("question_id", questionId);
            return this;
        }

        public QuestionAttemptRow sessionToken(String sessionToken) {
            columns.put("session_token", sessionToken);
            return this;
        }

        public QuestionAttemptRow body(String body) {
            columns.put("body", body);
            return this;
        }

        public QuestionAttemptRow createdAt(OffsetDateTime createdAt) {
            columns.put("created_at", createdAt);
            return this;
        }

        public void insert() {
            testDatabase.insert(QUESTION_ATTEMPT, columns);
        }
    }

    public final class DocumentRow {
        private final Map<String, Object> columns = new HashMap<>();

        DocumentRow() {}

        DocumentRow(String s3Key, String filename) {
            columns.put("s3_key", s3Key);
            columns.put("filename", filename);
        }

        public DocumentRow id(long id) {
            columns.put("id", id);
            return this;
        }

        public DocumentRow s3Key(String s3Key) {
            columns.put("s3_key", s3Key);
            return this;
        }

        public DocumentRow filename(String filename) {
            columns.put("filename", filename);
            return this;
        }

        public DocumentRow sourceSpec(String sourceSpec) {
            columns.put("source_spec", sourceSpec);
            return this;
        }

        public DocumentRow status(DocumentStatus status) {
            columns.put("status", status.name());
            return this;
        }

        public DocumentRow status(String status) {
            columns.put("status", status);
            return this;
        }

        public DocumentRow withoutStatus() {
            columns.put("status", null);
            return this;
        }

        public DocumentRow createdAt(OffsetDateTime createdAt) {
            columns.put("created_at", createdAt);
            return this;
        }

        public void insert() {
            testDatabase.insert(DOCUMENT, columns);
        }
    }

    public final class QuestionOriginRow {
        private final Map<String, Object> columns = new HashMap<>();

        QuestionOriginRow() {}

        QuestionOriginRow(long questionId, QuestionAuthorship authorship) {
            columns.put("question_id", questionId);
            columns.put("authorship", authorship.name());
        }

        public QuestionOriginRow id(long id) {
            columns.put("id", id);
            return this;
        }

        public QuestionOriginRow questionId(long questionId) {
            columns.put("question_id", questionId);
            return this;
        }

        public QuestionOriginRow authorship(QuestionAuthorship authorship) {
            columns.put("authorship", authorship.name());
            return this;
        }

        public QuestionOriginRow authorship(String authorship) {
            columns.put("authorship", authorship);
            return this;
        }

        public QuestionOriginRow documentId(long documentId) {
            columns.put("document_id", documentId);
            return this;
        }

        public QuestionOriginRow createdAt(OffsetDateTime createdAt) {
            columns.put("created_at", createdAt);
            return this;
        }

        public void insert() {
            testDatabase.insert(QUESTION_ORIGIN, columns);
        }
    }
}
