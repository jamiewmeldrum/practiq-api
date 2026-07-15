package com.practiq.service;

import com.practiq.domain.MarkScheme;
import com.practiq.dto.response.MarkSchemeResponse;
import com.practiq.repository.MarkSchemeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static utils.TestReflection.setField;

// A thin layer over the QuestionQueryManager visibility gate and MarkSchemeRepository. Both are mocked so
// these tests pin the orchestration: the question must be student-visible before the mark scheme is even
// looked up, and a missing scheme is an empty result, not an error.
@ExtendWith(MockitoExtension.class)
class MarkSchemeServiceTest {

    private static final long QUESTION_ID = 10L;

    @Mock
    private QuestionQueryManager questionQueryManager;

    @Mock
    private MarkSchemeRepository markSchemeRepository;

    @InjectMocks
    private MarkSchemeService markSchemeService;

    @Test
    void getForQuestionIdReturnsEmptyWhenQuestionNotVisibleToStudent() {
        when(questionQueryManager.doesStudentVisibleQuestionExistForId(QUESTION_ID)).thenReturn(false);

        Optional<MarkSchemeResponse> result = markSchemeService.getForQuestionId(QUESTION_ID);

        assertThat(result.isPresent(), equalTo(false));

        // The visibility gate short-circuits: an invisible question never reaches the mark-scheme lookup,
        // so its existence can't leak through a different not-found cause.
        verify(questionQueryManager).doesStudentVisibleQuestionExistForId(QUESTION_ID);
        verifyNoInteractions(markSchemeRepository);
    }

    @Test
    void getForQuestionIdReturnsEmptyWhenQuestionVisibleButHasNoMarkScheme() {
        when(questionQueryManager.doesStudentVisibleQuestionExistForId(QUESTION_ID)).thenReturn(true);
        when(markSchemeRepository.findByQuestionId(QUESTION_ID)).thenReturn(Optional.empty());

        Optional<MarkSchemeResponse> result = markSchemeService.getForQuestionId(QUESTION_ID);

        assertThat(result.isPresent(), equalTo(false));

        verify(questionQueryManager).doesStudentVisibleQuestionExistForId(QUESTION_ID);
        verify(markSchemeRepository).findByQuestionId(QUESTION_ID);
    }

    @Test
    void getForQuestionIdReturnsMappedResponseWhenQuestionVisibleAndMarkSchemeExists() {
        long markSchemeId = 1L;
        String body = "Award 1 mark for stating the wave bends around the edge of the gap.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        MarkScheme markScheme = new MarkScheme();
        setField(markScheme, "id", markSchemeId);
        setField(markScheme, "questionId", QUESTION_ID);
        setField(markScheme, "body", body);
        setField(markScheme, "createdAt", createdAt);

        when(questionQueryManager.doesStudentVisibleQuestionExistForId(QUESTION_ID)).thenReturn(true);
        when(markSchemeRepository.findByQuestionId(QUESTION_ID)).thenReturn(Optional.of(markScheme));

        Optional<MarkSchemeResponse> result = markSchemeService.getForQuestionId(QUESTION_ID);

        assertThat(result.isPresent(), equalTo(true));
        MarkSchemeResponse response = result.get();
        assertThat(response.getId(), equalTo(markSchemeId));
        assertThat(response.getQuestionId(), equalTo(QUESTION_ID));
        assertThat(response.getBody(), equalTo(body));
        assertThat(response.getCreatedAt(), equalTo(createdAt));

        verify(questionQueryManager).doesStudentVisibleQuestionExistForId(QUESTION_ID);
        verify(markSchemeRepository).findByQuestionId(QUESTION_ID);
    }
}
