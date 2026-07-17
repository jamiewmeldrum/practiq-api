package com.practiq.service;

import com.practiq.domain.Question;
import com.practiq.domain.projection.LinkedQuestion;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionSource;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import com.practiq.dto.request.QuestionRequest;
import com.practiq.dto.response.PageResponse;
import com.practiq.dto.response.QuestionResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static utils.TestReflection.setField;

// The service has two jobs: delegate retrieval to QuestionQueryManager, and assemble the response —
// mapping LinkedQuestion projections to QuestionResponse and pairing a page's metadata with its mapped
// content. The manager is mocked so both are pinned here; the mapper's own field-level nuance (null
// difficulty, empty links) belongs to QuestionResponseMapperTest.
@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionQueryManager questionQueryManager;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void getQuestionByIdReturnsEmptyWhenNoStudentVisibleQuestionExists() {
        long questionId = 1L;

        when(questionQueryManager.findStudentVisibleQuestionById(questionId)).thenReturn(Optional.empty());

        Optional<QuestionResponse> response = questionService.get(questionId);

        assertThat(response.isPresent(), equalTo(false));

        verify(questionQueryManager).findStudentVisibleQuestionById(questionId);
    }

    @Test
    void getQuestionByIdMapsTheLinkedQuestionToAResponse() {
        long questionId = 1L;
        long conceptId = 10L;
        String body = "State Newton's first law.";
        QuestionDifficulty difficulty = QuestionDifficulty.MEDIUM;
        QuestionType type = QuestionType.SHORT_ANSWER;
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        Question question = new Question(
                body, difficulty, type, QuestionSource.SEED, QuestionStatus.APPROVED, "AQA GCSE Physics");
        setField(question, "id", questionId);
        setField(question, "createdAt", createdAt);
        LinkedQuestion linkedQuestion =
                new LinkedQuestion(question, Set.of(new QuestionConceptLink(questionId, conceptId)));

        when(questionQueryManager.findStudentVisibleQuestionById(questionId))
                .thenReturn(Optional.of(linkedQuestion));

        Optional<QuestionResponse> response = questionService.get(questionId);

        assertThat(response.isPresent(), equalTo(true));
        QuestionResponse questionResponse = response.get();
        assertThat(questionResponse.getId(), equalTo(questionId));
        assertThat(questionResponse.getBody(), equalTo(body));
        assertThat(questionResponse.getDifficulty().getValue(), equalTo(difficulty.value()));
        assertThat(questionResponse.getDifficulty().getCode(), equalTo(difficulty.name()));
        assertThat(questionResponse.getType(), equalTo(type));
        assertThat(questionResponse.getCreatedAt(), equalTo(createdAt));
        assertThat(questionResponse.getLinkedConceptIds(), equalTo(Set.of(conceptId)));

        verify(questionQueryManager).findStudentVisibleQuestionById(questionId);
    }

    @Test
    void getQuestionsPairsThePagesMetadataWithItsMappedContent() {
        long questionId = 7L;
        String body = "Explain what is meant by diffraction.";

        Question question = new Question(
                body, null, null, QuestionSource.SEED, QuestionStatus.APPROVED, null);
        setField(question, "id", questionId);
        LinkedQuestion linkedQuestion = new LinkedQuestion(question, Set.of());

        QuestionRequest request = new QuestionRequest();
        Pageable requested = Pageable.from(2, 5);

        // Page number, size and total are all distinct, so an envelope that hardcodes a value or reads the
        // wrong field off the Page fails rather than coincidentally matching.
        Page<LinkedQuestion> page = Page.of(List.of(linkedQuestion), requested, 11L);
        when(questionQueryManager.findStudentVisibleQuestionsPagedAndFiltered(request, requested)).thenReturn(page);

        PageResponse<QuestionResponse> response = questionService.get(request, requested);

        // The content is the mapped question, not an empty list — the field-by-field mapping itself is
        // covered by getQuestionByIdMapsTheLinkedQuestionToAResponse.
        assertThat(response.content().size(), equalTo(1));
        assertThat(response.content().getFirst().getId(), equalTo(questionId));
        assertThat(response.content().getFirst().getBody(), equalTo(body));
        assertThat(response.page(), equalTo(2));
        assertThat(response.size(), equalTo(5));
        assertThat(response.totalCount(), equalTo(11L));

        verify(questionQueryManager).findStudentVisibleQuestionsPagedAndFiltered(request, requested);
    }
}
