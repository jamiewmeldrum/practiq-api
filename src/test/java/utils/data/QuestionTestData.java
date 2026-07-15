package utils.data;

import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class QuestionTestData extends TestData {

    public QuestionTestData(TestDatabase testDatabase) {
        super(testDatabase);
    }

    // Clears in FK-safe order: link rows reference questions and concepts, so they go first.
    @Override
    public void clear() {
        testDatabase.clear(MARK_SCHEME_TABLE);
        testDatabase.clear(QUESTION_CONCEPT_TABLE);
        testDatabase.clear(QUESTION_TABLE);
        testDatabase.clear(CONCEPT_TABLE);
    }

    public QuestionRow question() {
        return new QuestionRow();
    }

    public QuestionRow question(long id) {
        return new QuestionRow(id);
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
}
