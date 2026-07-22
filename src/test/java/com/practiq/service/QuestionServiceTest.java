package com.practiq.service;

import com.practiq.domain.Question;
import com.practiq.domain.projection.LinkedQuestion;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.domain.query.question.StudentQuestionQueryRunner;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static utils.TestReflection.setField;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private StudentQuestionQueryRunner questionQueryRunner;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void getQuestionByIdReturnsEmptyWhenNoVisibleQuestionExists() {
        long questionId = 1L;

        when(questionQueryRunner.findQuestionById(questionId)).thenReturn(Optional.empty());

        Optional<QuestionResponse> response = questionService.get(questionId);

        assertThat(response.isPresent(), equalTo(false));

        verify(questionQueryRunner).findQuestionById(questionId);
    }

    @Test
    void getQuestionByIdReturnsTheMappedQuestionWhenOneIsFound() {
        long questionId = 1L;

        Question question = new Question(
                "State Newton's first law.", QuestionDifficulty.MEDIUM, QuestionType.SHORT_ANSWER,
                QuestionSource.SEED, QuestionStatus.APPROVED, "AQA GCSE Physics");
        setField(question, "id", questionId);
        LinkedQuestion linkedQuestion = new LinkedQuestion(question, Set.of(new QuestionConceptLink(questionId, 10L)));

        when(questionQueryRunner.findQuestionById(questionId))
                .thenReturn(Optional.of(linkedQuestion));

        Optional<QuestionResponse> response = questionService.get(questionId);

        // The service's job is to present the found question, not to map its fields — the LinkedQuestion ->
        // QuestionResponse field mapping is QuestionResponseMapperTest's contract. Assert enough to know the
        // right question came through the mapper, no more.
        assertThat(response.isPresent(), equalTo(true));
        assertThat(response.get().getId(), equalTo(questionId));

        verify(questionQueryRunner).findQuestionById(questionId);
    }

    @Test
    void getQuestionsPairsThePagesMetadataWithItsMappedContent() {
        long questionId = 7L;
        String body = "Explain what is meant by diffraction.";

        Question question = new Question(
                body, null, null, QuestionSource.SEED, QuestionStatus.APPROVED, null);
        setField(question, "id", questionId);
        LinkedQuestion linkedQuestion = new LinkedQuestion(question, Set.of());

        // Distinct filter values so the assertion proves the service unpacks the request and hands the
        // runner each field, rather than passing something opaque through.
        List<QuestionType> types = List.of(QuestionType.MCQ);
        List<QuestionDifficulty> difficulties = List.of(QuestionDifficulty.HARD);
        long conceptId = 3L;
        QuestionRequest request = new QuestionRequest();
        request.setTypes(types);
        request.setDifficulties(difficulties);
        request.setConceptId(conceptId);

        Pageable requested = Pageable.from(2, 5);

        // Page number, size and total are all distinct, so an envelope that hardcodes a value or reads the
        // wrong field off the Page fails rather than coincidentally matching.
        Page<LinkedQuestion> page = Page.of(List.of(linkedQuestion), requested, 11L);
        when(questionQueryRunner.findQuestionsPagedAndFiltered(types, difficulties, conceptId, requested))
                .thenReturn(page);

        PageResponse<QuestionResponse> response = questionService.get(request, requested);

        // The content is the mapped question, not an empty list — the field-by-field mapping itself is
        // covered by getQuestionByIdMapsTheLinkedQuestionToAResponse.
        assertThat(response.content().size(), equalTo(1));
        assertThat(response.content().getFirst().getId(), equalTo(questionId));
        assertThat(response.content().getFirst().getBody(), equalTo(body));
        assertThat(response.page(), equalTo(2));
        assertThat(response.size(), equalTo(5));
        assertThat(response.totalCount(), equalTo(11L));

        verify(questionQueryRunner).findQuestionsPagedAndFiltered(types, difficulties, conceptId, requested);
    }
}
