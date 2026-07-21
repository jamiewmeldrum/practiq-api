package com.practiq.domain.query.question;

import com.practiq.domain.Question;
import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.types.QuestionType;
import com.practiq.repository.QuestionConceptRepository;
import com.practiq.repository.QuestionRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The runner mechanics (spec-from-policy, stitch, ordering, paging metadata) are proven once and
// policy-agnostic in QuestionQueryRunnerTest. This class proves only what is unique to the STUDENT runner:
// that the student policy is the one wired in, so the query reaching the spec factory always has the
// non-negotiables imposed — status = APPROVED and requiresConceptLink — while request filters pass through.
// One test per entry shape (by-id, catalogue); both share the same imposed policy via studentQuery(...).
@ExtendWith(MockitoExtension.class)
class StudentQuestionQueryRunnerTest {

    private static final QuerySpecification<Question> SPEC = (root, query, cb) -> null;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionConceptRepository questionConceptRepository;

    @Mock
    private QuestionSpecificationFactory questionSpecificationFactory;

    @InjectMocks
    private StudentQuestionQueryRunner studentQuestionQueryRunner;

    @Test
    void byIdPathImposesApprovedAndConceptLink() {
        long id = 7L;

        QuestionQuery expected = studentQuery()
                .questionId(id)
                .build();
        when(questionSpecificationFactory.forQuery(expected)).thenReturn(SPEC);
        when(questionRepository.findOne(SPEC)).thenReturn(Optional.empty());

        studentQuestionQueryRunner.findQuestionById(id);

        // If the wired policy failed to impose APPROVED + requiresConceptLink, this query wouldn't match
        // and the stubbed spec would never reach the repository.
        verify(questionSpecificationFactory).forQuery(expected);
    }

    @Test
    void cataloguePathImposesApprovedAndConceptLinkAndKeepsRequestFilters() {
        List<QuestionType> types = List.of(QuestionType.MCQ);
        List<QuestionDifficulty> difficulties = List.of(QuestionDifficulty.HARD);
        Long conceptId = 42L;

        QuestionQuery expected = studentQuery()
                .types(types)
                .difficulties(difficulties)
                .conceptId(conceptId)
                .build();
        when(questionSpecificationFactory.forQuery(expected)).thenReturn(SPEC);
        when(questionRepository.findAll(eq(SPEC), any(Pageable.class)))
                .thenReturn(Page.of(List.of(), Pageable.from(0), 0L));

        studentQuestionQueryRunner.findQuestionsPagedAndFiltered(types, difficulties, conceptId, Pageable.from(0, 20));

        verify(questionSpecificationFactory).forQuery(expected);
    }

    // The student non-negotiables, hand-built independently of StudentQuestionQueryPolicy so a regression
    // that dropped either would break these tests rather than move with the production code.
    private QuestionQuery.QuestionQueryBuilder studentQuery() {
        return QuestionQuery.builder()
                .status(QuestionStatus.APPROVED)
                .requiresConceptLink(true);
    }
}
