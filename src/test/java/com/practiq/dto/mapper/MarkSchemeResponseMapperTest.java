package com.practiq.dto.mapper;

import com.practiq.domain.MarkScheme;
import com.practiq.dto.response.MarkSchemeResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.practiq.dto.mapper.MarkSchemeResponseMapper.toMarkSchemeResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static utils.TestReflection.setField;

class MarkSchemeResponseMapperTest {

    @Test
    void markSchemeMapsToMarkSchemeResponse() {
        long id = 1L;
        long questionId = 10L;
        String body = "Diffraction";
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        MarkScheme markScheme = new MarkScheme();
        setField(markScheme, "id", id);
        setField(markScheme, "questionId", questionId);
        setField(markScheme, "body", body);
        setField(markScheme, "createdAt", createdAt);

        MarkSchemeResponse markSchemeResponse = toMarkSchemeResponse(markScheme);

        assertThat(markSchemeResponse.getId(), equalTo(id));
        assertThat(markSchemeResponse.getQuestionId(), equalTo(questionId));
        assertThat(markSchemeResponse.getBody(), equalTo(body));
        assertThat(markSchemeResponse.getCreatedAt(), equalTo(createdAt));
    }
}