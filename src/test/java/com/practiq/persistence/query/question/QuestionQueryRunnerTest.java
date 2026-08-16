package com.practiq.persistence.query.question;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static utils.TestReflection.setField;

import com.practiq.foundation.types.QuestionDifficulty;
import com.practiq.foundation.types.QuestionStatus;
import com.practiq.foundation.types.QuestionType;
import com.practiq.persistence.QuestionEntity;
import com.practiq.persistence.projection.QuestionConceptLinkProjection;
import com.practiq.persistence.repository.QuestionConceptRepository;
import com.practiq.persistence.repository.QuestionRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Tested with its real specification factory inside it and only the repositories mocked: the factory is an
// implementation detail the runner is exercised through, not a seam that earns its own test file.
@ExtendWith(MockitoExtension.class)
class QuestionQueryRunnerTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionConceptRepository questionConceptRepository;

    private QuestionQueryRunner runner;

    @BeforeEach
    void setUp() {
        runner = new QuestionQueryRunner(
                questionRepository, questionConceptRepository, new QuestionSpecificationFactory());
    }

    @Test
    void findAllAttachesEachQuestionsConceptIds() {
        QuestionEntity linked = question(1L, "Linked");
        QuestionEntity unlinked = question(2L, "Unlinked");

        when(questionRepository.findAll(any(QuerySpecification.class))).thenReturn(List.of(linked, unlinked));
        when(questionConceptRepository.findLinksByQuestionIds(any()))
                .thenReturn(List.of(
                        new QuestionConceptLinkProjection(1L, 10L), new QuestionConceptLinkProjection(1L, 11L)));

        List<QuestionWithConceptIds> found =
                runner.findAll(QuestionQuery.builder().build());

        assertThat(found.stream().map(q -> q.question().getBody()).toList(), contains("Linked", "Unlinked"));
        assertThat(found.getFirst().conceptIds(), containsInAnyOrder(10L, 11L));
        assertThat(found.getLast().conceptIds(), empty());
    }

    @Test
    void findAllSkipsTheLinkQueryWhenNothingMatched() {
        when(questionRepository.findAll(any(QuerySpecification.class))).thenReturn(List.of());

        assertThat(runner.findAll(QuestionQuery.builder().build()), empty());

        // A second statement for a miss would be a query issued with an empty id set.
        verifyNoInteractions(questionConceptRepository);
    }

    @Test
    void findPageImposesTheStableCreatedAtThenIdOrderOverTheRequestedPage() {
        QuestionEntity question = question(1L, "Question");

        when(questionRepository.findAll(any(QuerySpecification.class), any(Pageable.class)))
                .thenReturn(Page.of(List.of(question), Pageable.from(2, 5), 11L));
        when(questionConceptRepository.findLinksByQuestionIds(any()))
                .thenReturn(List.of(new QuestionConceptLinkProjection(1L, 10L)));

        Page<QuestionWithConceptIds> page =
                runner.findPage(QuestionQuery.builder().build(), Pageable.from(2, 5));

        ArgumentCaptor<Pageable> used = ArgumentCaptor.forClass(Pageable.class);
        verify(questionRepository).findAll(any(QuerySpecification.class), used.capture());

        assertThat(used.getValue().getNumber(), equalTo(2));
        assertThat(used.getValue().getSize(), equalTo(5));
        assertThat(used.getValue().getSort().getOrderBy(), contains(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));

        // The page's own metadata survives the mapping, so paging stays correct through the attachment.
        assertThat(page.getTotalSize(), equalTo(11L));
        assertThat(page.getContent().getFirst().conceptIds(), contains(10L));
    }

    @Test
    void existsAsksTheRepositoryWithoutFetchingRows() {
        when(questionRepository.exists(any(QuerySpecification.class))).thenReturn(true);

        assertThat(runner.exists(QuestionQuery.builder().build()), equalTo(true));

        verifyNoInteractions(questionConceptRepository);
    }

    private static QuestionEntity question(long id, String body) {
        QuestionEntity question =
                new QuestionEntity(body, QuestionDifficulty.EASY, QuestionType.MCQ, QuestionStatus.APPROVED);
        setField(question, "id", id);
        return question;
    }
}
