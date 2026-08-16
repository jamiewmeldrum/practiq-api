package com.practiq.web.dto.response;

import com.practiq.foundation.types.QuestionDifficulty;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;
import lombok.ToString;

@Serdeable
@Getter
@ToString
public class QuestionDifficultyResponse {
    private final int value;
    private final String code;

    public QuestionDifficultyResponse(QuestionDifficulty difficulty) {
        this.value = difficulty.value();
        this.code = difficulty.name();
    }
}
