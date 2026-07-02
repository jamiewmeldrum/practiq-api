package com.practiq.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;
import lombok.ToString;

@Serdeable
@Getter
@ToString
public class QuestionDifficultyDto {
    private final int value;
    private final String code;

    public QuestionDifficultyDto(int value, String code) {
        this.value = value;
        this.code = code;
    }
}
