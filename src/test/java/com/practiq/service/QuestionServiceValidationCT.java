package com.practiq.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.practiq.domain.types.QuestionType;
import com.practiq.dto.request.QuestionRequest;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import utils.ComponentTest;

@ComponentTest
class QuestionServiceValidationCT {

    @Inject
    private QuestionService questionService;

    @Test
    void getRejectsDuplicateTypesOnRequest() {
        QuestionRequest request = new QuestionRequest(List.of(QuestionType.MCQ, QuestionType.MCQ), null, null);

        ConstraintViolationException exception =
                assertThrows(ConstraintViolationException.class, () -> questionService.get(request, Pageable.UNPAGED));

        assertEquals("get.request.types: contains duplicates ([MCQ, MCQ])", exception.getMessage());
    }
}
