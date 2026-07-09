package com.practiq.dto.converter;

import com.practiq.domain.types.QuestionDifficulty;
import io.micronaut.core.convert.ConversionContext;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class QuestionDifficultyTypeConverterTest {

    private final ConversionContext context = mock(ConversionContext.class);
    private final QuestionDifficultyTypeConverter converter = new QuestionDifficultyTypeConverter();

    @Test
    void convertsDifficultyCodeToEnum() {
        Optional<QuestionDifficulty> result = converter.convert("3", QuestionDifficulty.class, context);

        assertEquals(Optional.of(QuestionDifficulty.MEDIUM), result);
        verifyNoInteractions(context);
    }

    // "6" parses cleanly but is outside the 1..5 range, so forValue returns null: the converter must
    // reject it, not surface a null difficulty into the bound list.
    @Test
    void rejectsParseableValueWithNoMatchingDifficulty() {
        Optional<QuestionDifficulty> result = converter.convert("6", QuestionDifficulty.class, context);

        assertEquals(Optional.empty(), result);
        verify(context).reject(eq("6"), any(IllegalArgumentException.class));
    }

    // A non-numeric value fails at parseInt, before forValue is ever reached: still a rejection, not a throw.
    @Test
    void rejectsNonIntegerValue() {
        Optional<QuestionDifficulty> result = converter.convert("BAD", QuestionDifficulty.class, context);

        assertEquals(Optional.empty(), result);
        verify(context).reject(eq("BAD"), any(NumberFormatException.class));
    }
}
