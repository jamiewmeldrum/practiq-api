package com.practiq.dto.response;

import com.practiq.domain.types.QuestionDifficulty;
import com.practiq.domain.types.QuestionType;
import utils.ComponentTest;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

// Uses the context-configured ObjectMapper (not ObjectMapper.getDefault()), so these tests exercise
// the real application.properties serialization config — including inclusion=always. That makes the
// null/empty test a genuine guard: drop that config line and it fails here, without needing Docker.
@ComponentTest
class QuestionResponseCT {

    @Inject
    private ObjectMapper objectMapper;

    @Test
    void questionDtoSerializesFull() throws IOException {
        Set<Long> linkedConceptIds = Set.of(10L, 11L);

        QuestionResponse question = new QuestionResponse(
                7,
                "State Newton's first law.",
                new QuestionDifficultyResponse(QuestionDifficulty.MEDIUM),
                QuestionType.EXTENDED,
                Instant.parse("2026-06-29T10:15:30Z"),
                linkedConceptIds
        );
        String actual = objectMapper.writeValueAsString(question);

        String expected = """
        {
          "id": 7,
          "body": "State Newton's first law.",
          "difficulty": { "value": 3, "code": "MEDIUM" },
          "type": "EXTENDED",
          "createdAt": "2026-06-29T10:15:30Z",
          "linkedConceptIds": [10, 11]
        }
        """;

        // NON_EXTENSIBLE: still rejects extra/renamed/missing fields, but ignores array order —
        // linkedConceptIds is a Set, so its serialized order is not part of the contract.
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void questionDtoIncludesNullAndEmptyFields() throws IOException {
        QuestionResponse question = new QuestionResponse(
                0,
                null,
                null,
                null,
                null,
                Set.of()
        );
        String actual = objectMapper.writeValueAsString(question);

        // inclusion=always: every field is present, nulls stay null and the empty set stays [].
        String expected = """
        {
          "id": 0,
          "body": null,
          "difficulty": null,
          "type": null,
          "createdAt": null,
          "linkedConceptIds": []
        }
        """;

        JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
    }
}
