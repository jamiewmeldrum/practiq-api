package com.practiq.dto.request;

import com.practiq.domain.types.QuestionType;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.validation.annotation.UniqueElements;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@EqualsAndHashCode
@Getter
@Setter
@Introspected
public class QuestionRequest {

    @Nullable
    @UniqueElements
    private List<QuestionType> types;
}
