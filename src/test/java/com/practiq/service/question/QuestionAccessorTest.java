package com.practiq.service.question;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.practiq.persistence.QuestionEntity;
import com.practiq.persistence.query.question.QuestionQuery;
import com.practiq.persistence.query.question.QuestionQueryRunner;
import com.practiq.persistence.query.question.QuestionWithConceptIds;
import com.practiq.service.question.policy.QuestionQueryPolicy;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

// The cardinality guarantee lives here and nowhere else: the runner cannot know a query matches at most one
// row, and no higher tier can provoke a breach because a primary key makes it unreachable end to end.
class QuestionAccessorTest {

    private final QuestionQueryRunner runner = mock(QuestionQueryRunner.class);
    private final QuestionQueryPolicy policy = mock(QuestionQueryPolicy.class);
    private final QuestionAccessor accessor = new QuestionAccessor(runner, policy);

    @Test
    void findByIdThrowsWhenTheQueryMatchedMoreThanOneQuestion() {
        long id = 17L;
        QuestionQuery query = QuestionQuery.builder().questionId(id).build();
        when(policy.forId(id)).thenReturn(query);
        when(runner.findAll(query)).thenReturn(List.of(questionWithConceptIds(1L), questionWithConceptIds(2L)));

        // Loud rather than returning the first of several: a policy that stopped filtering by id would
        // otherwise serve an arbitrary question under someone else's id.
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> accessor.findById(id));

        assertEquals("Expected at most one question for id 17, got 2", thrown.getMessage());
    }

    @Test
    void findByIdReturnsTheSingleMatch() {
        long id = 17L;
        QuestionQuery query = QuestionQuery.builder().questionId(id).build();
        QuestionWithConceptIds found = questionWithConceptIds(id);
        when(policy.forId(id)).thenReturn(query);
        when(runner.findAll(query)).thenReturn(List.of(found));

        assertThat(accessor.findById(id).orElseThrow(), is(found));
    }

    @Test
    void findByIdReturnsEmptyWhenNothingMatched() {
        long id = 17L;
        QuestionQuery query = QuestionQuery.builder().questionId(id).build();
        when(policy.forId(id)).thenReturn(query);
        when(runner.findAll(query)).thenReturn(List.of());

        assertThat(accessor.findById(id).isPresent(), is(false));
    }

    @Test
    void existsAsksTheRunnerWithThePolicysQueryForThatId() {
        long id = 17L;
        QuestionQuery query = QuestionQuery.builder().questionId(id).build();
        when(policy.forId(id)).thenReturn(query);
        when(runner.exists(query)).thenReturn(true);

        assertThat(accessor.exists(id), is(true));
    }

    private static QuestionWithConceptIds questionWithConceptIds(long id) {
        return new QuestionWithConceptIds(new QuestionEntity("A question.", null, null, null), Set.of(id));
    }
}
