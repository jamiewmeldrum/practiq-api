package com.practiq.service.question;

import com.practiq.persistence.query.question.QuestionQuery;
import com.practiq.persistence.query.question.QuestionQueryRunner;
import com.practiq.persistence.query.question.QuestionWithConceptIds;
import com.practiq.service.question.dto.request.QuestionSearchCriteria;
import com.practiq.service.question.policy.QuestionQueryPolicy;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import java.util.List;
import java.util.Optional;

// The one place a question query gets its policy. Instances are bound to a policy at construction (see
// QuestionAccessorFactory), so a caller holding the student accessor cannot make an admin read — the choice
// is made once, where the accessor is injected, rather than at each call.
public class QuestionAccessor {

    private final QuestionQueryRunner questionQueryRunner;
    private final QuestionQueryPolicy policy;

    public QuestionAccessor(QuestionQueryRunner questionQueryRunner, QuestionQueryPolicy policy) {
        this.questionQueryRunner = questionQueryRunner;
        this.policy = policy;
    }

    public boolean exists(long id) {
        return questionQueryRunner.exists(policy.forId(id));
    }

    public Optional<QuestionWithConceptIds> findById(long id) {
        List<QuestionWithConceptIds> found = questionQueryRunner.findAll(policy.forId(id));

        // The runner cannot know a query matches at most one row; this accessor can, because it built the
        // query. A policy that stopped filtering by id would otherwise return the first of many silently.
        if (found.size() > 1) {
            throw new IllegalStateException(
                    "Expected at most one question for id %s, got %s".formatted(id, found.size()));
        }

        return found.stream().findFirst();
    }

    public Page<QuestionWithConceptIds> findPage(QuestionSearchCriteria criteria, Pageable pageable) {
        QuestionQuery query = policy.catalogue(criteria.types(), criteria.difficulties(), criteria.conceptId());
        return questionQueryRunner.findPage(query, pageable);
    }
}
