package com.practiq.domain.query.question;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@EqualsAndHashCode
public class QuestionQuery {
    private List<QuestionType> types;
    private List<QuestionDifficulty> difficulties;
    private QuestionStatus status;
    private Long conceptId;
    private Long questionId;
    private boolean requiresConceptLink;
}
