package com.practiq.domain.query;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

// The web-agnostic, translated form of a question query. Holds domain values only — no request DTO —
// so this package never depends on dto/request. The service maps an incoming request into one of these.
// conceptId is nullable: null means "no concept filter", otherwise questions linked to that concept.
// requiresConceptLink is serving policy made explicit rather than buried in the specification factory:
// student-facing queries never serve an unlinked (unprocessed) question, but an admin review query must
// see exactly those. Redundant when conceptId is set — filtering by a concept already implies a link.
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

    // The student-catalogue policy in one place: only APPROVED questions, and only ones linked to at
    // least one concept. Constructing through here makes it impossible for a request-derived query to
    // widen what students can see.
    public static QuestionQuery studentCatalogue(List<QuestionType> types,
                                                 List<QuestionDifficulty> difficulties,
                                                 Long conceptId) {
        return QuestionQuery.builder()
                .types(types)
                .difficulties(difficulties)
                .status(QuestionStatus.APPROVED)
                .conceptId(conceptId)
                .requiresConceptLink(true)
                .build();
    }

    public static QuestionQuery studentCatalogue(long questionId) {
        return QuestionQuery.builder()
                .status(QuestionStatus.APPROVED)
                .questionId(questionId)
                .requiresConceptLink(true)
                .build();
    }
}
