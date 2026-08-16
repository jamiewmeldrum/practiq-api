package com.practiq.service.question.policy;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionType;
import com.practiq.persistence.query.question.QuestionQuery;
import java.util.List;

// Who may see what. A policy turns a request for questions into a query carrying the restrictions that
// audience is subject to, which is why it lives here and not beside the runner that executes it.
public interface QuestionQueryPolicy {

    QuestionQuery forId(long questionId);

    QuestionQuery catalogue(List<QuestionType> types, List<QuestionDifficulty> difficulties, Long conceptId);
}
