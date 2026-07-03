package com.practiq.dto.response;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;
import lombok.ToString;

@Serdeable
@Getter
@ToString
public class QuestionDifficultyResponse {
    private final int value;
    private final String code;

    public QuestionDifficultyResponse(int value, String code) {
        this.value = value;
        this.code = code;
    }
}
