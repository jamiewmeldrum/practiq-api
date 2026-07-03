package com.practiq.test;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

// Domain-aware question/concept fixtures for integration tests, layered over the generic
// TestDatabase JDBC helper. Centralises the schema knowledge — required columns and their
// defaults, the enum/difficulty storage conventions, and the FK-safe clear order — so a test
// arranges data in a line without repeating it. TestDatabase itself stays schema-agnostic.
@Singleton
public class QuestionTestData {

    private static final String QUESTION_TABLE = "question";
    private static final String QUESTION_CONCEPT_TABLE = "question_concept";
    private static final String CONCEPT_TABLE = "concept";

    private final TestDatabase testDatabase;

    public QuestionTestData(TestDatabase testDatabase) {
        this.testDatabase = testDatabase;
    }

    // Clears in FK-safe order: link rows reference questions and concepts, so they go first.
    public void clear() {
        testDatabase.clear(QUESTION_CONCEPT_TABLE);
        testDatabase.clear(QUESTION_TABLE);
        testDatabase.clear(CONCEPT_TABLE);
    }

    // Starts a question row with defaults for the required columns (body, source); override any
    // column via the fluent setters. Unset optional columns are omitted, so the DB applies NULL
    // or its own default (e.g. status -> PENDING).
    public QuestionRow question(long id) {
        return new QuestionRow(id);
    }

    public void concept(long id, String name, String description) {
        testDatabase.insert(
                CONCEPT_TABLE,
                Map.of(
                        "id", id,
                        "name", name,
                        "description", description
                )
        );
    }

    public void link(long questionId, long conceptId) {
        testDatabase.insert(
                QUESTION_CONCEPT_TABLE,
                Map.of(
                        "question_id", questionId,
                        "concept_id", conceptId
                )
        );
    }

    // Accumulates the columns for one question row, then writes it. Enum-backed columns are stored
    // as the constant name (upper-case, per project convention); difficulty is stored as its integer
    // value, matching QuestionDifficultyAttributeConverter.
    public final class QuestionRow {
        private final Map<String, Object> columns = new HashMap<>();

        private QuestionRow(long id) {
            columns.put("id", id);
            columns.put("body", "Question " + id);
            columns.put("source", QuestionSource.SEED.name());
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

        public QuestionRow type(QuestionType type) {
            columns.put("type", type.name());
            return this;
        }

        public QuestionRow status(QuestionStatus status) {
            columns.put("status", status.name());
            return this;
        }

        public QuestionRow sourceSpec(String sourceSpec) {
            columns.put("source_spec", sourceSpec);
            return this;
        }

        public void insert() {
            testDatabase.insert(QUESTION_TABLE, columns);
        }
    }
}
