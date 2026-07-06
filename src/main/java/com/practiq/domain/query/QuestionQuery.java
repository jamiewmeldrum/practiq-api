package com.practiq.domain.query;

import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;

import java.util.List;

// The web-agnostic, translated form of a question query. Holds domain values only — no request DTO —
// so this package never depends on dto/request. The service maps an incoming request into one of these.
public record QuestionQuery(List<QuestionType> types, QuestionStatus status) {

    public QuestionQuery {
        types = types == null ? List.of() : types;
    }
}
