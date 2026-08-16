package com.practiq.service.markscheme.mapper;

import static com.practiq.service.markscheme.mapper.MarkSchemeMapper.toMarkScheme;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static utils.TestReflection.setField;

import com.practiq.persistence.MarkSchemeEntity;
import com.practiq.service.markscheme.dto.response.MarkScheme;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MarkSchemeMapperTest {

    @Test
    void markSchemeEntityMapsToMarkScheme() {
        long id = 1L;
        int version = 6;
        long questionId = 10L;
        String body = "Award 1 mark for stating the wave bends around the edge of the gap.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        MarkSchemeEntity markSchemeEntity = new MarkSchemeEntity(questionId, body);
        setField(markSchemeEntity, "id", id);
        setField(markSchemeEntity, "version", version);
        setField(markSchemeEntity, "createdAt", createdAt);

        MarkScheme markScheme = toMarkScheme(markSchemeEntity);

        assertThat(markScheme.id(), equalTo(id));
        // The service model carries the lock token even though no web response exposes it: the caller of a
        // service method is not necessarily the web layer.
        assertThat(markScheme.version(), equalTo(version));
        assertThat(markScheme.questionId(), equalTo(questionId));
        assertThat(markScheme.body(), equalTo(body));
        assertThat(markScheme.createdAt(), equalTo(createdAt));
    }
}
