package utils.data;

import com.practiq.domain.types.*;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public abstract class TestData {

    public static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    protected static final String CONCEPT_TABLE = "concept";
    protected static final String QUESTION_TABLE = "question";
    protected static final String QUESTION_CONCEPT_TABLE = "question_concept";
    protected static final String MARK_SCHEME_TABLE = "mark_scheme";
    protected static final String QUESTION_ATTEMPT = "question_attempt";
    protected static final String DOCUMENT = "document";

    protected static final String ISO_8601_UTC = "\\d{4}-\\d{2}-\\d{2}T.*Z";

    protected final TestDatabase testDatabase;

    protected TestData(TestDatabase testDatabase) {
        this.testDatabase = testDatabase;
    }

    public abstract void clear();

    public String getInstantPattern() {
        return ISO_8601_UTC;
    }

    public final class QuestionRow {
        private final Map<String, Object> columns = new HashMap<>();

        QuestionRow() {}

        QuestionRow(long id) {
            columns.put("id", id);
            columns.put("body", "Question " + id);
            columns.put("source", QuestionSource.SEED.name());
        }

        public QuestionRow id(long id) {
            columns.put("id", id);
            return this;
        }

        public QuestionRow body(String body) {
            columns.put("body", body);
            return this;
        }

        public QuestionRow source(QuestionSource source) {
            columns.put("source", source.name());
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
            columns.put("status", status);
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
}
