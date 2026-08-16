package com.practiq.persistence.query.question;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionStatus;
import com.practiq.foundation.types.QuestionType;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

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
