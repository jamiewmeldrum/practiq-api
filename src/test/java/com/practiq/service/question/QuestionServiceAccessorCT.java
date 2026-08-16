package com.practiq.service.question;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.practiq.foundation.types.QuestionStatus;
import com.practiq.persistence.QuestionEntity;
import com.practiq.persistence.QuestionEntity_;
import com.practiq.persistence.repository.QuestionConceptRepository;
import com.practiq.persistence.repository.QuestionRepository;
import com.practiq.service.question.dto.request.QuestionSearchCriteria;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import io.micronaut.test.annotation.MockBean;
import jakarta.inject.Inject;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Subquery;
import java.util.List;
import org.junit.jupiter.api.Test;
import utils.ComponentTest;
import utils.CriteriaProbe;

// QuestionService is wired to a policy-bound accessor by qualifier alone. Get that wrong and every read
// silently widens — students receive PENDING questions with a 200 and a plausible body — so this proves the
// restrictions the injected accessor actually imposes, rather than that some accessor was called.
@ComponentTest
@SuppressWarnings("unchecked")
class QuestionServiceAccessorCT {

    @Inject
    private QuestionService questionService;

    @Inject
    private QuestionRepository questionRepository;

    @MockBean(QuestionRepository.class)
    QuestionRepository questionRepository() {
        return mock(QuestionRepository.class);
    }

    @MockBean(QuestionConceptRepository.class)
    QuestionConceptRepository questionConceptRepository() {
        return mock(QuestionConceptRepository.class);
    }

    @Test
    void readingAQuestionByIdImposesTheStudentRestrictions() {
        CriteriaProbe<QuestionEntity> probe = studentProbe();

        when(questionRepository.findAll(any(QuerySpecification.class))).thenAnswer(invocation -> {
            probe.resolve(invocation.getArgument(0));
            return List.of();
        });

        questionService.getById(17L);

        assertStudentRestrictions(probe);
    }

    @Test
    void readingTheCatalogueImposesTheStudentRestrictions() {
        CriteriaProbe<QuestionEntity> probe = studentProbe();

        when(questionRepository.findAll(any(QuerySpecification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    probe.resolve(invocation.getArgument(0));
                    return Page.of(List.of(), Pageable.from(0), 0L);
                });

        questionService.get(new QuestionSearchCriteria(null, null, null), Pageable.from(0));

        assertStudentRestrictions(probe);
    }

    private CriteriaProbe<QuestionEntity> studentProbe() {
        CriteriaProbe<QuestionEntity> probe = new CriteriaProbe<>();

        Path<QuestionStatus> statusPath = mock(Path.class);
        when(probe.root().get(QuestionEntity_.status)).thenReturn(statusPath);
        when(probe.criteriaQuery().subquery(Long.class)).thenReturn(mock(Subquery.class, RETURNS_DEEP_STUBS));

        return probe;
    }

    private void assertStudentRestrictions(CriteriaProbe<QuestionEntity> probe) {
        // APPROVED only, and only when a concept link exists — the two halves of the student serving policy.
        verify(probe.criteriaBuilder()).equal(probe.root().get(QuestionEntity_.status), QuestionStatus.APPROVED);
        verify(probe.criteriaQuery()).subquery(Long.class);
    }
}
