package com.practiq.service;

import com.practiq.domain.Concept;
import com.practiq.domain.Question;
import com.practiq.domain.QuestionConcept;
import com.practiq.domain.query.QuestionQuery;
import com.practiq.domain.query.QuestionSpecificationFactory;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import com.practiq.dto.request.QuestionRequest;
import com.practiq.dto.response.QuestionResponse;
import com.practiq.repository.QuestionRepository;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static com.practiq.test.TestReflection.setField;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    // Sentinel handed back by the mocked factory so we can assert this exact instance reaches the repo.
    // Never executed — the repo is mocked — so the body is irrelevant.
    private static final QuerySpecification<Question> SPEC = (root, query, cb) -> null;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionSpecificationFactory questionSpecificationFactory;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void getForcesApprovedStatusAndRunsTheBuiltSpec() {
        QuestionRequest request = new QuestionRequest();
        QuestionQuery approvedOnly = new QuestionQuery(List.of(), QuestionStatus.APPROVED);
        when(questionSpecificationFactory.from(approvedOnly)).thenReturn(SPEC);
        when(questionRepository.findAll(SPEC)).thenReturn(List.of());

        List<QuestionResponse> questions = questionService.get(request);

        assertEquals(0, questions.size());

        // Status pinned to APPROVED, and the exact spec the factory produced is what gets executed.
        verify(questionSpecificationFactory).from(approvedOnly);
        verify(questionRepository).findAll(SPEC);
    }

    @Test
    void getBuildsQueryFromRequestTypesAndForcesApprovedStatus() {
        QuestionRequest request = new QuestionRequest();
        request.setTypes(List.of(QuestionType.SHORT_ANSWER, QuestionType.MCQ));

        // The request's types must reach the factory verbatim, paired with the hard-coded APPROVED
        // status. Exact-arg stub-then-verify: QuestionQuery is a record, so equality checks every field.
        QuestionQuery expectedQuery = new QuestionQuery(
                List.of(QuestionType.SHORT_ANSWER, QuestionType.MCQ), QuestionStatus.APPROVED);
        when(questionSpecificationFactory.from(expectedQuery)).thenReturn(SPEC);
        when(questionRepository.findAll(SPEC)).thenReturn(List.of());

        questionService.get(request);

        verify(questionSpecificationFactory).from(expectedQuery);
        verify(questionRepository).findAll(SPEC);
    }

    @Test
    void getMapsQuestionsIncludingDifficultyAndConceptLinks() {
        long linkedId = 1L;
        String linkedBody = "Explain what is meant by diffraction.";
        QuestionDifficulty linkedDifficulty = QuestionDifficulty.MEDIUM;
        QuestionType linkedType = QuestionType.EXTENDED;
        QuestionSource linkedSource = QuestionSource.SEED;
        QuestionStatus linkedStatus = QuestionStatus.APPROVED;
        String linkedSourceSpec = "AQA GCSE Physics";
        Instant linkedCreatedAt = Instant.parse("2026-06-29T10:15:30Z");
        long conceptIdOne = 10L;
        long conceptIdTwo = 11L;

        Question linkedQuestion = new Question(
                linkedBody, linkedDifficulty, linkedType, linkedSource, linkedStatus, linkedSourceSpec);
        setField(linkedQuestion, "id", linkedId);
        setField(linkedQuestion, "createdAt", linkedCreatedAt);
        setField(linkedQuestion, "conceptLinks", Set.of(
                link(linkedQuestion, conceptIdOne),
                link(linkedQuestion, conceptIdTwo)
        ));

        long bareId = 2L;
        String bareBody = "Define displacement.";
        QuestionType bareType = QuestionType.SHORT_ANSWER;
        QuestionSource bareSource = QuestionSource.EXTRACTED;
        QuestionStatus bareStatus = QuestionStatus.PENDING;
        String bareSourceSpec = "OCR A-Level Physics";
        Instant bareCreatedAt = Instant.parse("2026-06-30T08:00:00Z");

        // Difficulty deliberately null and no concept links, to cover the null/empty mapping paths.
        Question bareQuestion = new Question(
                bareBody, null, bareType, bareSource, bareStatus, bareSourceSpec);
        setField(bareQuestion, "id", bareId);
        setField(bareQuestion, "createdAt", bareCreatedAt);

        when(questionSpecificationFactory.from(any())).thenReturn(SPEC);
        when(questionRepository.findAll(SPEC)).thenReturn(List.of(linkedQuestion, bareQuestion));

        QuestionRequest request = new QuestionRequest();
        List<QuestionResponse> questions = questionService.get(request);

        assertEquals(2, questions.size());

        QuestionResponse linked = questionById(questions, linkedId);
        assertEquals(linkedBody, linked.getBody());
        assertThat(linked.getDifficulty(), is(notNullValue()));
        assertEquals(linkedDifficulty.value(), linked.getDifficulty().getValue());
        assertEquals(linkedDifficulty.name(), linked.getDifficulty().getCode());
        assertEquals(linkedType, linked.getType());
        assertEquals(linkedSource, linked.getSource());
        assertEquals(linkedStatus, linked.getStatus());
        assertEquals(linkedSourceSpec, linked.getSourceSpec());
        assertEquals(linkedCreatedAt, linked.getCreatedAt());
        assertThat(linked.getLinkedConceptIds(), containsInAnyOrder(conceptIdOne, conceptIdTwo));

        QuestionResponse bare = questionById(questions, bareId);
        assertEquals(bareBody, bare.getBody());
        assertThat(bare.getDifficulty(), is(nullValue()));
        assertEquals(bareType, bare.getType());
        assertEquals(bareSource, bare.getSource());
        assertEquals(bareStatus, bare.getStatus());
        assertEquals(bareSourceSpec, bare.getSourceSpec());
        assertEquals(bareCreatedAt, bare.getCreatedAt());
        assertThat(bare.getLinkedConceptIds(), is(empty()));
    }

    private static QuestionResponse questionById(List<QuestionResponse> questions, long id) {
        return questions.stream()
                .filter(question -> question.getId() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No question with id " + id));
    }

    // Builds a concept-link row for a question that already has its id set (QuestionConcept reads
    // both ids on construction). The concept detail is irrelevant to the mapping, which only reads
    // the concept id back off the link.
    private static QuestionConcept link(Question question, long conceptId) {
        Concept concept = new Concept("Concept " + conceptId, "Description " + conceptId);
        setField(concept, "id", conceptId);
        return new QuestionConcept(question, concept);
    }
}
