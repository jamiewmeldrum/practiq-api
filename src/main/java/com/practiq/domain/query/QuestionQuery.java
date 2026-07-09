package com.practiq.domain.query;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;

import java.util.List;

// The web-agnostic, translated form of a question query. Holds domain values only — no request DTO —
// so this package never depends on dto/request. The service maps an incoming request into one of these.
// conceptId is nullable: null means "no concept filter", otherwise questions linked to that concept.
// requiresConceptLink is serving policy made explicit rather than buried in the specification factory:
// student-facing queries never serve an unlinked (unprocessed) question, but an admin review query must
// see exactly those. Redundant when conceptId is set — filtering by a concept already implies a link.
public record QuestionQuery(List<QuestionType> types,
                            List<QuestionDifficulty> difficulties,
                            QuestionStatus status,
                            Long conceptId,
                            boolean requiresConceptLink) {

    public QuestionQuery {
        types = types == null ? List.of() : types;
        difficulties = difficulties == null ? List.of() : difficulties;
    }

    // The student-catalogue policy in one place: only APPROVED questions, and only ones linked to at
    // least one concept. Constructing through here makes it impossible for a request-derived query to
    // widen what students can see.
    public static QuestionQuery studentCatalogue(List<QuestionType> types,
                                                 List<QuestionDifficulty> difficulties,
                                                 Long conceptId) {
        return new QuestionQuery(types, difficulties, QuestionStatus.APPROVED, conceptId, true);
    }
}
