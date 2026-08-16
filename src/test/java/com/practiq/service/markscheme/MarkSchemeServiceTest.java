package com.practiq.service.markscheme;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;
import static utils.TestReflection.setField;

import com.practiq.persistence.MarkSchemeEntity;
import com.practiq.persistence.repository.MarkSchemeRepository;
import com.practiq.service.markscheme.dto.response.MarkScheme;
import com.practiq.service.question.QuestionAccessor;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// A thin layer over the QuestionAccessor visibility gate and MarkSchemeRepository. Both are mocked so
// these tests pin the orchestration: the question must be student-visible before the mark scheme is even
// looked up, and a missing scheme is an empty result, not an error.
@ExtendWith(MockitoExtension.class)
class MarkSchemeServiceTest {

    @Mock
    private QuestionAccessor questions;

    @Mock
    private MarkSchemeRepository markSchemeRepository;

    @InjectMocks
    private MarkSchemeService markSchemeService;

    @Test
    void getForQuestionIdReturnsEmptyWhenQuestionDoesNotExistForQuery() {
        long questionId = 10L;

        when(questions.exists(questionId)).thenReturn(false);

        Optional<MarkScheme> result = markSchemeService.getForQuestionId(questionId);

        assertThat(result.isPresent(), equalTo(false));

        // The visibility gate short-circuits: an invisible question never reaches the mark-scheme lookup,
        // so its existence can't leak through a different not-found cause.
        verify(questions).exists(questionId);
        verifyNoInteractions(markSchemeRepository);
    }

    @Test
    void getForQuestionIdReturnsEmptyWhenQuestionVisibleButHasNoMarkScheme() {
        long questionId = 10L;

        when(questions.exists(questionId)).thenReturn(true);
        when(markSchemeRepository.findByQuestionId(questionId)).thenReturn(Optional.empty());

        Optional<MarkScheme> result = markSchemeService.getForQuestionId(questionId);

        assertThat(result.isPresent(), equalTo(false));

        verify(questions).exists(questionId);
        verify(markSchemeRepository).findByQuestionId(questionId);
    }

    @Test
    void getForQuestionIdReturnsMappedMarkSchemeWhenQuestionVisibleAndMarkSchemeExists() {
        long questionId = 10L;
        long markSchemeId = 1L;
        int version = 5;
        String body = "Award 1 mark for stating the wave bends around the edge of the gap.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        MarkSchemeEntity markSchemeEntity = new MarkSchemeEntity(questionId, body);
        setField(markSchemeEntity, "id", markSchemeId);
        setField(markSchemeEntity, "version", version);
        setField(markSchemeEntity, "createdAt", createdAt);

        when(questions.exists(questionId)).thenReturn(true);
        when(markSchemeRepository.findByQuestionId(questionId)).thenReturn(Optional.of(markSchemeEntity));

        Optional<MarkScheme> result = markSchemeService.getForQuestionId(questionId);

        assertThat(result.isPresent(), equalTo(true));
        // Built here rather than through MarkSchemeMapper: an expected value produced by the code under test
        // moves with it and can never fail.
        assertThat(result.get(), equalTo(new MarkScheme(markSchemeId, version, questionId, body, createdAt)));

        verify(questions).exists(questionId);
        verify(markSchemeRepository).findByQuestionId(questionId);
    }
}
