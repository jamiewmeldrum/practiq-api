package com.practiq.web.dto.mapper;

import static com.practiq.web.dto.mapper.MarkSchemeResponseMapper.toMarkSchemeResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.practiq.service.markscheme.dto.response.MarkScheme;
import com.practiq.web.dto.response.MarkSchemeResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MarkSchemeResponseMapperTest {

    @Test
    void markSchemeMapsToMarkSchemeResponse() {
        long id = 1L;
        long questionId = 10L;
        String body = "Award 1 mark for stating the wave bends around the edge of the gap.";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        // A non-zero version the response has no field for: the web mapper selects a subset, so the lock
        // token must be dropped here rather than reaching a client.
        MarkScheme markScheme = new MarkScheme(id, 8, questionId, body, createdAt);

        MarkSchemeResponse markSchemeResponse = toMarkSchemeResponse(markScheme);

        assertThat(markSchemeResponse.id(), equalTo(id));
        assertThat(markSchemeResponse.questionId(), equalTo(questionId));
        assertThat(markSchemeResponse.body(), equalTo(body));
        assertThat(markSchemeResponse.createdAt(), equalTo(createdAt));
    }
}
