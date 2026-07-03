package com.practiq.dto.request;

import com.practiq.domain.types.QuestionStatus;
import io.micronaut.core.annotation.Introspected;
import lombok.Getter;

@Getter
@Introspected
public class QuestionRequest {
    private QuestionStatus status = QuestionStatus.APPROVED;
}
