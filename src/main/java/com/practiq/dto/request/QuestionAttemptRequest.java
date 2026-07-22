package com.practiq.dto.request;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
@Getter
@Setter
@Introspected
public class QuestionAttemptRequest {

    @NotBlank
    private String body;
}
