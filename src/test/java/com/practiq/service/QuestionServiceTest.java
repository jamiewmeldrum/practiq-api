package com.practiq.service;

import com.practiq.dto.request.QuestionRequest;
import com.practiq.dto.response.PageResponse;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Currently very low value tests as everything delegated to QuestionQueryManager. Tests exist only
 * to ensure testing is considered if changes do occur - a lack of a test class can lead to someone
 * deciding tests aren't needed for the class by default which is best avoided.
 */
@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionQueryManager questionQueryManager;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void getQuestionByIdDelegatesToQueryManager() {
        long id = 1L;
        when(questionQueryManager.findQuestionByIdForStudent(id)).thenReturn(Optional.empty());

        questionService.get(id);

        verify(questionQueryManager).findQuestionByIdForStudent(id);
    }

    @Test
    void getQuestionsForRequestDelegatesToQueryManager() {
        QuestionRequest request = new QuestionRequest();
        Pageable pageable = Pageable.from(0, 20);

        when(questionQueryManager.findQuestionsPagedAndFilteredForStudent(request, pageable))
                .thenReturn(new PageResponse<>(List.of(), 0, 0, 0));

        questionService.get(request, pageable);

        verify(questionQueryManager).findQuestionsPagedAndFilteredForStudent(request, pageable);
    }
}
